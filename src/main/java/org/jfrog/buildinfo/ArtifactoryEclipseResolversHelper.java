package org.jfrog.buildinfo;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.Authentication;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.repository.Proxy;
import org.eclipse.aether.RepositorySystemSession;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.List;
import java.util.Properties;

public class ArtifactoryEclipseResolversHelper {

    private ResolutionHelper resolutionHelper;

    private Log logger;

    private List<ArtifactRepository> resolutionRepositories = null;
    private ArtifactRepository releaseRepository = null;
    private ArtifactRepository snapshotRepository = null;

    public ArtifactoryEclipseResolversHelper(Log logger, MavenSession session, ArtifactoryClientConfiguration clientConfiguration) {
        this.logger = logger;
        Properties allMavenProps = new Properties() {{
            putAll(session.getSystemProperties());
            putAll(session.getUserProperties());
        }};
        this.resolutionHelper = new ResolutionHelper(logger, allMavenProps, clientConfiguration);
    }

    void initResolutionRepositories() {
        getResolutionRepositories();
    }

    List<ArtifactRepository> getResolutionRepositories() {
        if (resolutionRepositories == null) {
            List<ArtifactRepository> tempRepositories = Lists.newArrayList();

            String releaseRepoUrl = resolutionHelper.getRepoReleaseUrl();
            String snapshotRepoUrl = resolutionHelper.getRepoSnapshotUrl();

            Authentication authentication = null;
            if (StringUtils.isNotBlank(resolutionHelper.getRepoUsername())) {
                authentication = new org.apache.maven.artifact.repository.Authentication(resolutionHelper.getRepoUsername(), resolutionHelper.getRepoPassword());
            }
            Proxy proxy = null;
            if (StringUtils.isNotBlank(resolutionHelper.getProxyHost())) {
                proxy = new org.apache.maven.repository.Proxy();
                proxy.setHost(resolutionHelper.getProxyHost());
                proxy.setPort(resolutionHelper.getProxyPort());
                proxy.setUserName(resolutionHelper.getProxyUsername());
                proxy.setPassword(resolutionHelper.getProxyPassword());
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
                if (proxy != null) {
                    logger.debug("[buildinfo] Enforcing proxy: " + proxy + " for snapshot resolution repository");
                    snapshotRepository.setProxy(proxy);
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
                if (proxy != null) {
                    logger.debug("[buildinfo] Enforcing proxy: " + proxy + " for release resolution repository");
                    releasePluginRepository.setProxy(proxy);
                }
                tempRepositories.add(releasePluginRepository);
            }

            resolutionRepositories = tempRepositories;
        }
        return resolutionRepositories;
    }

    ArtifactRepository getSnapshotRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories();

        if (snapshotRepository != null) {
            return snapshotRepository;
        }
        return releaseRepository;
    }

    ArtifactRepository getReleaseRepository(RepositorySystemSession session) {
        // Init repositories configured in the Artifactory plugin:
        initResolutionRepositories();

        return releaseRepository;
    }

}
