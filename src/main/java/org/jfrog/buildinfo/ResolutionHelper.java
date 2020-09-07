package org.jfrog.buildinfo;

import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.Properties;

/**
 * @author yahavi
 */
public class ResolutionHelper {

    private final ArtifactoryClientConfiguration internalConfiguration;

    public ResolutionHelper(Log logger, Properties allMavenProps, ArtifactoryClientConfiguration clientConfiguration) {
        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps, new MavenBuildInfoLogger(logger));
        this.internalConfiguration = clientConfiguration;
        internalConfiguration.fillFromProperties(allProps);
    }

    public String getRepoReleaseUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getRepoKey());
    }

    public String getRepoSnapshotUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getDownloadSnapshotRepoKey());
    }

    public String getRepoUsername() {
        return internalConfiguration.resolver.getUsername();
    }

    public String getRepoPassword() {
        return internalConfiguration.resolver.getPassword();
    }

    public String getProxyHost() {
        return internalConfiguration.proxy.getHost();
    }

    public Integer getProxyPort() {
        return internalConfiguration.proxy.getPort();
    }

    public String getProxyUsername() {
        return internalConfiguration.proxy.getUsername();
    }

    public String getProxyPassword() {
        return internalConfiguration.proxy.getPassword();
    }
}
