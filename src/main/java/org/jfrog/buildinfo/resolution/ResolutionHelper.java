package org.jfrog.buildinfo.resolution;

import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.buildinfo.utils.MavenBuildInfoLogger;

import java.util.Properties;

/**
 * Resolve Artifactory URL, username, password and repositories from system properties or the pom.
 *
 * @author yahavi
 */
public class ResolutionHelper {

    private final ArtifactoryClientConfiguration clientConfiguration;

    public ResolutionHelper(Log logger, Properties allMavenProps, ArtifactoryClientConfiguration clientConfiguration) {
        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps, new MavenBuildInfoLogger(logger));
        this.clientConfiguration = clientConfiguration;
        this.clientConfiguration.fillFromProperties(allProps);
    }

    public String getRepoReleaseUrl() {
        return clientConfiguration.resolver.getUrl(clientConfiguration.resolver.getRepoKey());
    }

    public String getRepoSnapshotUrl() {
        return clientConfiguration.resolver.getUrl(clientConfiguration.resolver.getDownloadSnapshotRepoKey());
    }

    public String getRepoUsername() {
        return clientConfiguration.resolver.getUsername();
    }

    public String getRepoPassword() {
        return clientConfiguration.resolver.getPassword();
    }
}