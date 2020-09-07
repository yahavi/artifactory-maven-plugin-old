package org.jfrog.buildinfo;

import org.apache.commons.collections4.MapUtils;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.api.Artifact;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.ModuleParallelDeployHelper;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;
import org.jfrog.build.extractor.clientConfiguration.deploy.DeployDetails;
import org.jfrog.build.extractor.retention.Utils;

import java.io.File;
import java.util.*;

import static org.jfrog.buildinfo.Utils.setChecksums;

/**
 * @author yahavi
 */
public class BuildDeployer {

    private final Log logger;

    public BuildDeployer(Log logger) {
        this.logger = logger;
    }

    public void deploy(Build build, ArtifactoryClientConfiguration clientConf, Map<String, DeployDetails> deployableArtifactBuilders) {
        Map<String, Set<DeployDetails>> deployableArtifactsByModule = prepareDeployableArtifacts(build, deployableArtifactBuilders);

        logger.debug("Build Info Recorder: deploy artifacts: " + clientConf.publisher.isPublishArtifacts());
        logger.debug("Build Info Recorder: publication fork count: " + clientConf.publisher.getPublishForkCount());
        logger.debug("Build Info Recorder: publish build info: " + clientConf.publisher.isPublishBuildInfo());

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
        if (MapUtils.isEmpty(deployableArtifacts)) {
            logger.info("Artifactory Build Info Recorder: no artifacts to deploy...");
            return false;
        }
        return true;
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
                        setChecksums(file, artifact, logger);
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
}
