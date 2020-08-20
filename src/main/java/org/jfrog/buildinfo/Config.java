package org.jfrog.buildinfo;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.apache.commons.lang3.ClassUtils;
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

//    public static class Resolver {
//        @Delegate
//        ArtifactoryClientConfiguration.ResolverHandler delegate = CLIENT_CONFIGURATION.resolver;
//    }

//    public static class Publisher {
//        @Delegate(types = {
//                ArtifactoryClientConfiguration.PublisherHandler.class,
//                ArtifactoryClientConfiguration.RepositoryConfiguration.class,
//                ArtifactoryClientConfiguration.AuthenticationConfiguration.class})
//        ArtifactoryClientConfiguration.PublisherHandler delegate = CLIENT_CONFIGURATION.publisher;
//    }

    public static class Publisher {
        @Delegate(types = {ArtifactoryClientConfiguration.PublisherHandler.class})
        ArtifactoryClientConfiguration.PublisherHandler delegate = CLIENT_CONFIGURATION.publisher;
    }

    public static class BuildInfo {
        @Delegate
        ArtifactoryClientConfiguration.BuildInfoHandler delegate = CLIENT_CONFIGURATION.info;
    }

//    static class LicenseControl {
//        @Delegate
//        ArtifactoryClientConfiguration.LicenseControlHandler delegate = CLIENT_CONFIGURATION.info.licenseControl;
//    }
//
//    static class IssuesTracker {
//        @Delegate
//        ArtifactoryClientConfiguration.IssuesTrackerHandler delegate = CLIENT_CONFIGURATION.info.issues;
//    }
}
