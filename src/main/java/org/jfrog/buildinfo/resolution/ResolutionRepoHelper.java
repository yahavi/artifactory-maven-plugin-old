package org.jfrog.buildinfo.resolution;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.List;
import java.util.Properties;

public class ResolutionRepoHelper {

    private List<ArtifactRepository> resolutionRepositories;
    private final ResolutionHelper resolutionHelper;
    private final Log logger;

    public ResolutionRepoHelper(Log logger, MavenSession session, ArtifactoryClientConfiguration clientConfiguration) {
        this.logger = logger;
        Properties allMavenProps = new Properties() {{
            putAll(session.getSystemProperties());
            putAll(session.getUserProperties());
        }};
        this.resolutionHelper = new ResolutionHelper(logger, allMavenProps, clientConfiguration);
        setResolutionRepositories();
    }

    private void setResolutionRepositories() {
        List<ArtifactRepository> tempRepositories = Lists.newArrayList();

        String releaseRepoUrl = resolutionHelper.getRepoReleaseUrl();
        String snapshotRepoUrl = resolutionHelper.getRepoSnapshotUrl();

        Authentication authentication = null;
        if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
            authentication = new Authentication(resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword());
        }

        if (StringUtils.isNotBlank(snapshotRepoUrl)) {
            logger.debug("[buildinfo] Enforcing snapshot repository for resolution: " + snapshotRepoUrl);
            ArtifactRepositoryPolicy releasePolicy = new ArtifactRepositoryPolicy(false, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
            ArtifactRepositoryPolicy snapshotPolicy = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
            ArtifactRepository snapshotRepository = new MavenArtifactRepository("artifactory-snapshot", snapshotRepoUrl, new DefaultRepositoryLayout(), snapshotPolicy, releasePolicy);
            if (authentication != null) {
                logger.debug("[buildinfo] Enforcing repository authentication: " + authentication + " for snapshot resolution repository");
                snapshotRepository.setAuthentication(authentication);
            }

            if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
                authentication = new Authentication(resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword());
            }
            tempRepositories.add(snapshotRepository);
        }

        if (StringUtils.isNotBlank(releaseRepoUrl)) {
            logger.debug("[buildinfo] Enforcing release repository for resolution: " + releaseRepoUrl);
            boolean snapshotPolicyEnabled = tempRepositories.isEmpty();
            String repositoryId = snapshotPolicyEnabled ? "artifactory-release-snapshot" : "artifactory-release";

            ArtifactRepositoryPolicy releasePolicy = new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
            ArtifactRepositoryPolicy snapshotPolicy = new ArtifactRepositoryPolicy(snapshotPolicyEnabled, ArtifactRepositoryPolicy.UPDATE_POLICY_DAILY, ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
            ArtifactRepository releasePluginRepository = new MavenArtifactRepository(repositoryId, releaseRepoUrl, new DefaultRepositoryLayout(), snapshotPolicy, releasePolicy);
            if (authentication != null) {
                logger.debug("[buildinfo] Enforcing repository authentication: " + authentication + " for release resolution repository");
                releasePluginRepository.setAuthentication(authentication);
            }
            tempRepositories.add(releasePluginRepository);
        }

        resolutionRepositories = tempRepositories;
    }

    public List<ArtifactRepository> getResolutionRepositories() {
        return resolutionRepositories;
    }
}
