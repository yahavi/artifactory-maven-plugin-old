package org.jfrog.buildinfo;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.BuildInfoConfigProperties;
import org.jfrog.build.api.Module;
import org.jfrog.build.api.util.FileChecksumCalculator;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.ModuleParallelDeployHelper;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployableArtifactsUtils;
import org.jfrog.build.extractor.retention.Utils;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author yahavi
 */
public class BuildDeployer {

    private final Log logger;

    public BuildDeployer(Log logger) {
        this.logger = logger;
    }

    public void deploy(Build build, ArtifactoryClientConfiguration clientConf, Map<String, DeployDetails> deployableArtifactBuilders, File basedir) {
        Map<String, Set<DeployDetails>> deployableArtifactsByModule = prepareDeployableArtifacts(build, deployableArtifactBuilders);

        logger.debug("Build Info Recorder: deploy artifacts: " + clientConf.publisher.isPublishArtifacts());
        logger.debug("Build Info Recorder: publication fork count: " + clientConf.publisher.getPublishForkCount());
        logger.debug("Build Info Recorder: publish build info: " + clientConf.publisher.isPublishBuildInfo());

        if (clientConf.publisher.isPublishBuildInfo() || StringUtils.isNotBlank(clientConf.info.getGeneratedBuildInfoFilePath())) {
            saveBuildInfoToFile(build, clientConf, basedir);
        }

        if (!StringUtils.isEmpty(clientConf.info.getGeneratedBuildInfoFilePath())) {
            try {
                BuildInfoExtractorUtils.saveBuildInfoToFile(build, new File(clientConf.info.getGeneratedBuildInfoFilePath()));
            } catch (Exception e) {
                logger.error("Failed writing build info to file: ", e);
                throw new RuntimeException("Failed writing build info to file", e);
            }
        }

        if (!StringUtils.isEmpty(clientConf.info.getDeployableArtifactsFilePath())) {
            try {
                DeployableArtifactsUtils.saveDeployableArtifactsToFile(deployableArtifactsByModule, new File(clientConf.info.getDeployableArtifactsFilePath()), false);
            } catch (Exception e) {
                logger.error("Failed writing deployable artifacts to file: ", e);
                throw new RuntimeException("Failed writing deployable artifacts to file", e);
            }
        }

        BuildInfoClientBuilder buildInfoClientBuilder = new BuildInfoClientBuilder(logger);
        if (isDeployArtifacts(clientConf, deployableArtifactsByModule)) {
            try (ArtifactoryBuildInfoClient client = buildInfoClientBuilder.resolveProperties(clientConf)) {
                new ModuleParallelDeployHelper().deployArtifacts(client, deployableArtifactsByModule, clientConf.publisher.getPublishForkCount());
            }
        }
        if (clientConf.publisher.isPublishBuildInfo()) {
            publishBuildInfo(clientConf, build, buildInfoClientBuilder);
            return;
        }
        logger.info("Artifactory Build Info Recorder: publish build info set to false, build info will not be published...");
    }

    private void publishBuildInfo(ArtifactoryClientConfiguration clientConf, Build build, BuildInfoClientBuilder buildInfoClientBuilder) {
        try (ArtifactoryBuildInfoClient client = buildInfoClientBuilder.resolveProperties(clientConf)) {
            logger.info("Artifactory Build Info Recorder: Deploying build info ...");
            Utils.sendBuildAndBuildRetention(client, build, clientConf);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isDeployArtifacts(ArtifactoryClientConfiguration clientConf, Map<String, Set<DeployDetails>> deployableArtifacts) {
        if (!clientConf.publisher.isPublishArtifacts()) {
            logger.info("Artifactory Build Info Recorder: deploy artifacts set to false, artifacts will not be deployed...");
            return false;
        }
        if (deployableArtifacts == null || deployableArtifacts.isEmpty()) {
            logger.info("Artifactory Build Info Recorder: no artifacts to deploy...");
            return false;
        }
        return true;
    }

    private void saveBuildInfoToFile(Build build, ArtifactoryClientConfiguration clientConf, File basedir) {
        String outputFile = clientConf.getExportFile();
        File buildInfoFile = StringUtils.isBlank(outputFile) ? new File(basedir, "target/build-info.json") :
                new File(outputFile);

        logger.debug("Build Info Recorder: " + BuildInfoConfigProperties.EXPORT_FILE + " = " + outputFile);
        logger.info("Artifactory Build Info Recorder: Saving Build Info to '" + buildInfoFile + "'");

        try {
            BuildInfoExtractorUtils.saveBuildInfoToFile(build, buildInfoFile.getCanonicalFile());
        } catch (IOException e) {
            throw new RuntimeException("Error occurred while persisting Build Info to '" + buildInfoFile + "'", e);
        }
    }

    private Map<String, Set<DeployDetails>> prepareDeployableArtifacts(Build build, Map<String, DeployDetails> deployableArtifactBuilders) {
        Map<String, Set<DeployDetails>> deployableArtifactsByModule = new LinkedHashMap<>();
        List<Module> modules = build.getModules();
        for (Module module : modules) {
            Set<DeployDetails> moduleDeployableArtifacts = new LinkedHashSet<>();
            List<Artifact> artifacts = module.getArtifacts();
            if (artifacts != null) {
                for (Artifact artifact : artifacts) {
                    String artifactId = BuildInfoExtractorUtils.getArtifactId(module.getId(), artifact.getName());
                    DeployDetails deployable = deployableArtifactBuilders.get(artifactId);
                    if (deployable != null) {
                        File file = deployable.getFile();
                        setArtifactChecksums(file, artifact);
                        artifact.setRemotePath(deployable.getTargetRepository() + "/" + deployable.getArtifactPath());
                        moduleDeployableArtifacts.add(new DeployDetails.Builder().
                                artifactPath(deployable.getArtifactPath()).
                                file(file).
                                md5(artifact.getMd5()).
                                sha1(artifact.getSha1()).
                                addProperties(deployable.getProperties()).
                                targetRepository(deployable.getTargetRepository()).
                                packageType(DeployDetails.PackageType.MAVEN).
                                build());
                    }
                }
            }
            if (!moduleDeployableArtifacts.isEmpty()) {
                deployableArtifactsByModule.put(module.getId(), moduleDeployableArtifacts);
            }
        }
        return deployableArtifactsByModule;
    }

    private void setArtifactChecksums(File artifactFile, org.jfrog.build.api.Artifact artifact) {
        if ((artifactFile != null) && (artifactFile.isFile())) {
            try {
                Map<String, String> checksums = FileChecksumCalculator.calculateChecksums(artifactFile, "md5", "sha1");
                artifact.setMd5(checksums.get("md5"));
                artifact.setSha1(checksums.get("sha1"));
            } catch (Exception e) {
                logger.error("Could not set checksum values on '" + artifact.getName() + "': " + e.getMessage(), e);
            }
        }
    }

}
