package org.jfrog.buildinfo;

import org.apache.commons.lang.StringUtils;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;
import org.jfrog.build.api.util.Log;
import org.jfrog.build.extractor.BuildInfoExtractorUtils;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.util.Properties;

/**
 * Created by liorh on 4/24/14.
 */

public class ResolutionHelper {

    @Requirement
    private Logger logger;
    private ArtifactoryClientConfiguration internalConfiguration;
    private boolean initialized = false;

    public void init(Properties allMavenProps) {
        if (internalConfiguration != null) {
            return;
        }

        Log log = new MavenBuildInfoLogger(logger);
        Properties allProps = BuildInfoExtractorUtils.mergePropertiesWithSystemAndPropertyFile(allMavenProps, log);
        internalConfiguration = new ArtifactoryClientConfiguration(log);
        internalConfiguration.fillFromProperties(allProps);
        initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Determines a deployed artifact's scope (either "project" or "build") according to the maven's request context sent as an argument.
     *
     * @param requestContext The deployed artifact's request context.
     * @return Scope value for the request context.
     */
    public String getScopeByRequestContext(String requestContext) {
        if (requestContext == null) {
            return "project";
        }
        if ("plugin".equals(requestContext)) {
            return "build";
        }
        return "project";
    }

    public String getRepoReleaseUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getRepoKey());
    }

    public String getRepoSnapshotUrl() {
        return internalConfiguration.resolver.getUrl(internalConfiguration.resolver.getDownloadSnapshotRepoKey());
    }

    /**
     * Checks whether Artifactory resolution repositories have been configured.
     *
     * @return true if at least one Artifactory resolution repository has been configured.
     */
    public boolean resolutionRepositoriesConfigured() {
        return StringUtils.isNotBlank(getRepoReleaseUrl()) || StringUtils.isNotBlank(getRepoSnapshotUrl());
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
