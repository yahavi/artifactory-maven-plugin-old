package org.jfrog.buildinfo;

import org.jfrog.build.api.util.NullLog;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.PrefixPropertyHandler;

/**
 * @author yahavi
 */
public class Config {
    private static final ArtifactoryClientConfiguration CLIENT_CONFIGURATION = new ArtifactoryClientConfiguration(new NullLog());

    interface DelegatesToPrefixPropertyHandler {
        PrefixPropertyHandler getDelegate();
    }

    public static class Artifactory {
        public ArtifactoryClientConfiguration delegate = CLIENT_CONFIGURATION;
    }

    public static class Resolver implements DelegatesToPrefixPropertyHandler {
        public PrefixPropertyHandler getDelegate() {
            return CLIENT_CONFIGURATION.resolver;
        }
    }

    public static class Publisher implements DelegatesToPrefixPropertyHandler {
        public PrefixPropertyHandler getDelegate() {
            return CLIENT_CONFIGURATION.publisher;
        }
    }

    public static class BuildInfo implements DelegatesToPrefixPropertyHandler {
        public PrefixPropertyHandler getDelegate() {
            return CLIENT_CONFIGURATION.info;
        }
    }

    public static class LicenseControl implements DelegatesToPrefixPropertyHandler {
        public PrefixPropertyHandler getDelegate() {
            return CLIENT_CONFIGURATION.info.licenseControl;
        }
    }

    public static class IssuesTracker implements DelegatesToPrefixPropertyHandler {
        public PrefixPropertyHandler getDelegate() {
            return CLIENT_CONFIGURATION.info.issues;
        }
    }
}
