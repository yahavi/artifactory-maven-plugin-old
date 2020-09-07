package org.jfrog.buildinfo;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.repository.legacy.metadata.ArtifactMetadata;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.builder.ArtifactBuilder;
import org.jfrog.build.api.builder.BuildInfoMavenBuilder;
import org.jfrog.build.api.builder.DependencyBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.BuildInfoExtractor;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.IncludeExcludePatterns;
import org.jfrog.build.extractor.clientConfiguration.PatternMatcher;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.buildinfo.types.ModuleArtifacts;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getModuleIdString;
import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getTypeString;

/**
 * @author yahavi
 */
public class ArtifactoryExecutionListener extends AbstractExecutionListener implements BuildInfoExtractor<ExecutionEvent> {

    private final Map<String, DeployDetails> deployableArtifactBuilderMap = Maps.newConcurrentMap();
    private final Set<Artifact> resolvedArtifacts = Collections.synchronizedSet(new HashSet<>());
    private final ModuleArtifacts currentModuleDependencies = new ModuleArtifacts();
    private final ModuleArtifacts currentModuleArtifacts = new ModuleArtifacts();
    private final ThreadLocal<ModuleBuilder> currentModule = new ThreadLocal<>();
    private final ArtifactoryClientConfiguration conf;
    private final BuildInfoMavenBuilder buildInfoBuilder;
    private final BuildDeployer buildDeployer;

    private final Log logger;

    public ArtifactoryExecutionListener(MavenSession session, Log logger, ArtifactoryClientConfiguration conf) {
        this.buildInfoBuilder = new BuildInfoModelPropertyResolver(logger, session, conf);
        this.buildDeployer = new BuildDeployer(logger);
        this.logger = logger;
        this.conf = conf;
    }

    @Override
    public void projectSucceeded(ExecutionEvent event) {
        MavenProject project = event.getProject();
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info module initialization: Null project.");
            return;
        }

        ModuleBuilder moduleBuilder = new ModuleBuilder();
        moduleBuilder.id(getModuleIdString(project.getGroupId(), project.getArtifactId(), project.getVersion()));
        moduleBuilder.properties(project.getProperties());

        // Fill currentModuleArtifacts
        addArtifacts(project);

        // Fill currentModuleDependencies
        addDependencies(project);

        // Build module
        addArtifactsToCurrentModule(project, moduleBuilder);
        addDependenciesToCurrentModule(moduleBuilder);
        buildInfoBuilder.addModule(moduleBuilder.build());

        // Clean up
        currentModule.remove();
        currentModuleArtifacts.remove();
        currentModuleDependencies.remove();
        resolvedArtifacts.clear();
    }

    @Override
    public void mojoSucceeded(ExecutionEvent event) {
        addDependencies(event.getProject());
    }

    @Override
    public void mojoFailed(ExecutionEvent event) {
        addDependencies(event.getProject());
    }

    /**
     * Build and publish build info
     *
     * @param event - The execution event
     */
    @Override
    public void sessionEnded(ExecutionEvent event) {
        Build build = extract(event);
        if (build != null) {
            File basedir = event.getSession().getTopLevelProject().getBasedir();
            buildDeployer.deploy(build, conf, deployableArtifactBuilderMap, basedir);
        }
        deployableArtifactBuilderMap.clear();
    }

    @Override
    public Build extract(ExecutionEvent event) {
        MavenSession session = event.getSession();
        if (session.getResult().hasExceptions()) {
            return null;
        }
        if (conf.isIncludeEnvVars()) {
            Properties envProperties = new Properties();
            envProperties.putAll(conf.getAllProperties());
            envProperties = BuildInfoExtractorUtils.getEnvProperties(envProperties, conf.getLog());
            envProperties.forEach(buildInfoBuilder::addProperty);
        }
        long time = new Date().getTime() - session.getRequest().getStartTime().getTime();
        return buildInfoBuilder.durationMillis(time).build();
    }

    public void artifactResolved(Artifact artifact) {
        if (artifact != null) {
            resolvedArtifacts.add(artifact);
        }
    }

    private void addArtifacts(MavenProject project) {
        currentModuleArtifacts.add(project.getArtifact());
        currentModuleArtifacts.addAll(project.getAttachedArtifacts());
    }

    private void addDependencies(MavenProject project) {
        Set<Artifact> dependencies = Sets.newHashSet();
        for (Artifact artifact : project.getArtifacts()) {
            String classifier = StringUtils.defaultString(artifact.getClassifier(), "");
            String scope = StringUtils.defaultIfBlank(artifact.getScope(), Artifact.SCOPE_COMPILE);
            Artifact art = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(),
                    artifact.getVersion(), scope, artifact.getType(), classifier, artifact.getArtifactHandler());
            art.setFile(artifact.getFile());
            dependencies.add(art);
        }
        Set<Artifact> moduleDependencies = currentModuleDependencies.getOrCreate();
        Set<Artifact> tempSet = Sets.newHashSet(moduleDependencies);
        moduleDependencies.clear();
        moduleDependencies.addAll(dependencies);
        moduleDependencies.addAll(tempSet);
        if (conf.publisher.isRecordAllDependencies()) {
            moduleDependencies.addAll(resolvedArtifacts);
        }
    }

    private void addArtifactsToCurrentModule(MavenProject project, ModuleBuilder moduleBuilder) {
        Set<Artifact> artifacts = currentModuleArtifacts.get();

        ArtifactoryClientConfiguration.PublisherHandler publisher = conf.publisher;
        IncludeExcludePatterns patterns = new IncludeExcludePatterns(publisher.getIncludePatterns(), publisher.getExcludePatterns());
        boolean excludeArtifactsFromBuild = publisher.isFilterExcludedArtifactsFromBuild();

        boolean pomFileAdded = false;
        Artifact nonPomArtifact = null;
        String pomFileName = null;

        for (Artifact moduleArtifact : artifacts) {
            String artifactId = moduleArtifact.getArtifactId();
            String artifactVersion = moduleArtifact.getVersion();
            String artifactClassifier = moduleArtifact.getClassifier();
            String artifactExtension = moduleArtifact.getArtifactHandler().getExtension();
            String type = getTypeString(moduleArtifact.getType(), artifactClassifier, artifactExtension);
            String artifactName = getArtifactName(artifactId, artifactVersion, artifactClassifier, artifactExtension);
            File artifactFile = moduleArtifact.getFile();

            if ("pom".equals(type)) {
                pomFileAdded = true;
                // For pom projects take the file from the project if the artifact file is null.
                if (moduleArtifact.equals(project.getArtifact())) {
                    // project.getFile() returns the project pom file
                    artifactFile = project.getFile();
                }
            } else {
                boolean pomExist = moduleArtifact.getMetadataList().stream()
                        .anyMatch(artifactMetadata -> artifactMetadata instanceof ProjectArtifactMetadata);
                if (pomExist) {
                    nonPomArtifact = moduleArtifact;
                    pomFileName = StringUtils.removeEnd(artifactName, artifactExtension) + "pom";
                }
            }

            org.jfrog.build.api.Artifact artifact = new ArtifactBuilder(artifactName).type(type).build();
            String groupId = moduleArtifact.getGroupId();
            String deploymentPath = getDeploymentPath(groupId, artifactId, artifactVersion, artifactClassifier, artifactExtension);
            if (artifactFile != null && artifactFile.isFile()) {
                boolean pathConflicts = PatternMatcher.pathConflicts(deploymentPath, patterns);
                addArtifactToBuildInfo(artifact, pathConflicts, excludeArtifactsFromBuild, moduleBuilder);
                addDeployableArtifact(moduleBuilder, artifact, artifactFile, pathConflicts, moduleArtifact.getGroupId(), artifactId, artifactVersion, artifactClassifier, artifactExtension);
            }
        }
        /*
         * In case of non packaging Pom project module, we need to create the pom file from the ProjectArtifactMetadata on the Artifact
         */
        if (!pomFileAdded && nonPomArtifact != null) {
            String deploymentPath = getDeploymentPath(
                    nonPomArtifact.getGroupId(),
                    nonPomArtifact.getArtifactId(),
                    nonPomArtifact.getVersion(),
                    nonPomArtifact.getClassifier(), "pom");
            addPomArtifact(moduleBuilder, nonPomArtifact, moduleBuilder, patterns, deploymentPath, pomFileName, excludeArtifactsFromBuild);
        }
    }

    private void addDependenciesToCurrentModule(ModuleBuilder moduleBuilder) {
        Set<Artifact> dependencies = currentModuleDependencies.getOrCreate();
        for (Artifact dependency : dependencies) {
            File depFile = dependency.getFile();
            DependencyBuilder dependencyBuilder = new DependencyBuilder()
                    .id(getModuleIdString(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))
                    .type(getTypeString(dependency.getType(), dependency.getClassifier(), getExtension(depFile)));
            String scopes = dependency.getScope();
            if (StringUtils.isNotBlank(scopes)) {
                dependencyBuilder.scopes(Sets.newHashSet(scopes));
            }
            setDependencyChecksums(depFile, dependencyBuilder);
            moduleBuilder.addDependency(dependencyBuilder.build());
        }
    }

    private void setDependencyChecksums(File dependencyFile, DependencyBuilder dependencyBuilder) {
        if (dependencyFile == null || !dependencyFile.isFile()) {
            return;
        }
        try {
            Map<String, String> checksumsMap = FileChecksumCalculator.calculateChecksums(dependencyFile, "md5", "sha1");
            dependencyBuilder.md5(checksumsMap.get("md5"));
            dependencyBuilder.sha1(checksumsMap.get("sha1"));
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("Could not set checksum values on '" + dependencyBuilder.build().getId() + "': " + e.getMessage(), e);
        }
    }

    private String getExtension(File depFile) {
        String extension = StringUtils.EMPTY;
        if (depFile == null) {
            return extension;
        }
        String fileName = depFile.getName();
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot + 1 < fileName.length()) {
            extension = fileName.substring(lastDot + 1);
        }
        return extension;
    }

    private void addPomArtifact(ModuleBuilder moduleBuilder, Artifact nonPomArtifact, ModuleBuilder module,
                                IncludeExcludePatterns patterns, String deploymentPath, String pomFileName, boolean excludeArtifactsFromBuild) {
        for (ArtifactMetadata metadata : nonPomArtifact.getMetadataList()) {
            if (!(metadata instanceof ProjectArtifactMetadata)) { // The pom metadata
                continue;
            }
            ArtifactBuilder artifactBuilder = new ArtifactBuilder(pomFileName).type("pom");
            File pomFile = ((ProjectArtifactMetadata) metadata).getFile();
            org.jfrog.build.api.Artifact pomArtifact = artifactBuilder.build();

            if (pomFile != null && pomFile.isFile()) {
                boolean pathConflicts = PatternMatcher.pathConflicts(deploymentPath, patterns);
                addArtifactToBuildInfo(pomArtifact, pathConflicts, excludeArtifactsFromBuild, module);
                addDeployableArtifact(moduleBuilder, pomArtifact, pomFile, pathConflicts, nonPomArtifact.getGroupId(), nonPomArtifact.getArtifactId(), nonPomArtifact.getVersion(), nonPomArtifact.getClassifier(), "pom");
            }
            return;
        }
    }

    private String getArtifactName(String artifactId, String version, String classifier, String fileExtension) {
        String name = String.join("-", artifactId, version);
        if (StringUtils.isNotBlank(classifier)) {
            name += "-" + classifier;
        }
        return name + "." + fileExtension;
    }

    private String getDeploymentPath(String groupId, String artifactId, String version, String classifier,
                                     String fileExtension) {
        return String.join("/", groupId.replace(".", "/"), artifactId, version, getArtifactName(artifactId, version, classifier, fileExtension));
    }

    /**
     * If excludeArtifactsFromBuild and the PatternMatcher found conflicts, add the excluded artifact to the excluded artifacts list in the build info.
     * Otherwise, add the artifact to the regular artifacts list.
     */
    private void addArtifactToBuildInfo(org.jfrog.build.api.Artifact artifact, boolean pathConflicts, boolean isFilterExcludedArtifactsFromBuild, ModuleBuilder module) {
        if (isFilterExcludedArtifactsFromBuild && pathConflicts) {
            module.addExcludedArtifact(artifact);
        } else {
            module.addArtifact(artifact);
        }
    }

    private void addDeployableArtifact(ModuleBuilder moduleBuilder, org.jfrog.build.api.Artifact artifact, File artifactFile, boolean pathConflicts,
                                       String groupId, String artifactId, String version, String classifier, String fileExtension) {
        if (pathConflicts) {
            logger.info("'" + artifact.getName() + "' will not be deployed due to the defined include-exclude patterns.");
            return;
        }
        String deploymentPath = getDeploymentPath(groupId, artifactId, version, classifier, fileExtension);
        // deploy to snapshots or releases repository based on the deploy version
        String targetRepository = getTargetRepository(deploymentPath);

        DeployDetails deployable = new DeployDetails.Builder()
                .artifactPath(deploymentPath)
                .file(artifactFile)
                .targetRepository(targetRepository)
                .addProperties(conf.publisher.getMatrixParams())
                .packageType(DeployDetails.PackageType.MAVEN).build();
        String myArtifactId = BuildInfoExtractorUtils.getArtifactId(moduleBuilder.build().getId(), artifact.getName());

        deployableArtifactBuilderMap.put(myArtifactId, deployable);
    }

    /**
     * @param deployPath the full path string to extract the repo from
     * @return Return the target deployment repository. Either the releases
     * repository (default) or snapshots if defined and the deployed file is a
     * snapshot.
     */
    public String getTargetRepository(String deployPath) {
        String snapshotsRepository = conf.publisher.getSnapshotRepoKey();
        if (snapshotsRepository != null && deployPath.contains("-SNAPSHOT")) {
            return snapshotsRepository;
        }
        return conf.publisher.getRepoKey();
    }
}
