package org.jfrog.buildinfo;

import lombok.experimental.Delegate;
import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.PrefixPropertyHandler;

/**
 * @author yahavi
 */
public class Config {

    private static final ArtifactoryClientConfiguration CLIENT_CONFIGURATION = new ArtifactoryClientConfiguration(new NullLog());

    public static class Artifactory {
        @Delegate
        ArtifactoryClientConfiguration delegate = CLIENT_CONFIGURATION;
    }

    @SuppressWarnings("unused")
    public static class Resolver {
        @Delegate(types = {
                ArtifactoryClientConfiguration.ResolverHandler.class,
                ArtifactoryClientConfiguration.RepositoryConfiguration.class,
                ArtifactoryClientConfiguration.AuthenticationConfiguration.class,
                PrefixPropertyHandler.class})
        ArtifactoryClientConfiguration.ResolverHandler delegate = CLIENT_CONFIGURATION.resolver;
    }

    @SuppressWarnings("unused")
    public static class Publisher {
        @Delegate(types = {
                ArtifactoryClientConfiguration.PublisherHandler.class,
                ArtifactoryClientConfiguration.RepositoryConfiguration.class,
                ArtifactoryClientConfiguration.AuthenticationConfiguration.class,
                PrefixPropertyHandler.class})
        ArtifactoryClientConfiguration.PublisherHandler delegate = CLIENT_CONFIGURATION.publisher;
    }

    public static class BuildInfo {
        @Delegate(types = {ArtifactoryClientConfiguration.BuildInfoHandler.class, PrefixPropertyHandler.class})
        ArtifactoryClientConfiguration.BuildInfoHandler delegate = CLIENT_CONFIGURATION.info;
    }
}
