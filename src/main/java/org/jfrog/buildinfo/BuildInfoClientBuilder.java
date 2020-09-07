package org.jfrog.buildinfo;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ClientConfigurationFields;
import org.jfrog.build.extractor.clientConfiguration.client.ArtifactoryBuildInfoClient;

import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_CONNECTION_RETRIES;
import static org.jfrog.build.extractor.clientConfiguration.ClientProperties.PROP_TIMEOUT;

/**
 * Simple class to build {@link ArtifactoryBuildInfoClient} for deployment.
 *
 * @author yahavi
 */
public class BuildInfoClientBuilder {

    private final Log logger;

    public BuildInfoClientBuilder(Log logger) {
        this.logger = logger;
    }

    public ArtifactoryBuildInfoClient resolveProperties(ArtifactoryClientConfiguration clientConf) {
        ArtifactoryBuildInfoClient client = resolveClientProps(clientConf);
        resolveTimeout(clientConf, client);
        resolveProxy(clientConf.proxy, client);
        resolveRetriesParams(clientConf, client);
        resolveInsecureTls(clientConf, client);
        return client;
    }

    private ArtifactoryBuildInfoClient resolveClientProps(ArtifactoryClientConfiguration clientConf) {
        String contextUrl = clientConf.publisher.getContextUrl();
        if (StringUtils.isBlank(contextUrl)) {
            throw new IllegalArgumentException("Unable to resolve Artifactory Build Info Client properties: no context URL was found.");
        }
        logResolvedProperty(clientConf.publisher.getPrefix() + "." + ClientConfigurationFields.CONTEXT_URL, contextUrl);

        String username = clientConf.publisher.getUsername();
        String password = clientConf.publisher.getPassword();
        if (StringUtils.isNotBlank(username)) {
            logResolvedProperty(ClientConfigurationFields.USERNAME, username);
            return new ArtifactoryBuildInfoClient(contextUrl, username, password, new MavenBuildInfoLogger(logger));
        }
        return new ArtifactoryBuildInfoClient(contextUrl, new MavenBuildInfoLogger(logger));
    }

    private void resolveTimeout(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        if (clientConf.getTimeout() == null) {
            return;
        }
        int timeout = clientConf.getTimeout();
        logResolvedProperty(PROP_TIMEOUT, String.valueOf(timeout));
        client.setConnectionTimeout(timeout);
    }

    private void resolveRetriesParams(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        if (clientConf.getConnectionRetries() == null) {
            return;
        }
        int configMaxRetries = clientConf.getConnectionRetries();
        logResolvedProperty(PROP_CONNECTION_RETRIES, String.valueOf(configMaxRetries));
        client.setConnectionRetries(configMaxRetries);
    }

    private void resolveInsecureTls(ArtifactoryClientConfiguration clientConf, ArtifactoryBuildInfoClient client) {
        client.setInsecureTls(clientConf.getInsecureTls());
    }

    private void resolveProxy(ArtifactoryClientConfiguration.ProxyHandler proxyConf,
                              ArtifactoryBuildInfoClient client) {
        String proxyHost = proxyConf.getHost();
        if (StringUtils.isBlank(proxyHost)) {
            return;
        }
        logResolvedProperty(ClientConfigurationFields.HOST, proxyHost);
        if (proxyConf.getPort() == null) {
            return;
        }
        String proxyUsername = proxyConf.getUsername();
        if (StringUtils.isNotBlank(proxyUsername)) {
            logResolvedProperty(ClientConfigurationFields.USERNAME, proxyUsername);
            client.setProxyConfiguration(proxyHost, proxyConf.getPort(), proxyUsername, proxyConf.getPassword());
        } else {
            client.setProxyConfiguration(proxyHost, proxyConf.getPort());
        }
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Client Property Resolver: " + key + " = " + value);
    }
}
