package org.jfrog.buildinfo.resolution;

import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.buildinfo.utils.MavenBuildInfoLogger;

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
}