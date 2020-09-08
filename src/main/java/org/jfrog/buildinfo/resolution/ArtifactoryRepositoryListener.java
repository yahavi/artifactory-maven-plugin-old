package org.jfrog.buildinfo.resolution;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.codehaus.plexus.logging.Logger;
import org.eclipse.aether.AbstractRepositoryListener;
import org.eclipse.aether.RepositoryEvent;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.jfrog.buildinfo.deployment.BuildInfoRecorder;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @author yahavi
 */
@Named
@Singleton
public class ArtifactoryRepositoryListener extends AbstractRepositoryListener {

    private BuildInfoRecorder executionListener;

    /**
     * Empty constructor for serialization
     */
    @SuppressWarnings("unused")
    public ArtifactoryRepositoryListener() {
    }

    /**
     * Constructor for tests
     *
     * @param logger - The logger
     */
    public ArtifactoryRepositoryListener(Logger logger) {
        this.logger = logger;
    }

    @SuppressWarnings("unused")
    @Inject
    private Logger logger;

    public void setExecutionListener(BuildInfoRecorder executionListener) {
        this.executionListener = executionListener;
    }

    @Override
    public void artifactResolved(RepositoryEvent event) {
        if (executionListener == null) {
            return;
        }
        String requestContext = ((ArtifactRequest) event.getTrace().getData()).getRequestContext();
        String scope = getScopeByRequestContext(requestContext);
        org.apache.maven.artifact.Artifact artifact = toMavenArtifact(event.getArtifact(), scope);
        if (event.getRepository() != null) {
            logger.debug("[buildinfo] Resolved artifact: " + artifact + " from: " + event.getRepository() + ". Context is: " + requestContext);
            executionListener.artifactResolved(artifact);
            return;
        }
        logger.debug("[buildinfo] Could not resolve artifact: " + artifact);
    }

    /**
     * Converts org.eclipse.aether.artifact.Artifact objects into org.apache.maven.artifact.Artifact objects.
     */
    private Artifact toMavenArtifact(final org.eclipse.aether.artifact.Artifact art, String scope) {
        if (art == null) {
            return null;
        }
        String classifier = StringUtils.defaultString(art.getClassifier());
        DefaultArtifact artifact = new DefaultArtifact(art.getGroupId(), art.getArtifactId(), art.getVersion(), scope, art.getExtension(), classifier, null);
        artifact.setFile(art.getFile());
        return artifact;
    }

    public String getScopeByRequestContext(String requestContext) {
        return StringUtils.equals(requestContext, "plugin") ? "build" : "project";
    }
}
