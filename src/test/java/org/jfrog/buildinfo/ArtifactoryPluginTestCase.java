package org.jfrog.buildinfo;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.*;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;

import java.io.File;
import java.util.Collections;

/**
 * @author yahavi
 */
public abstract class ArtifactoryPluginTestCase extends AbstractMojoTestCase {

    @Override
    protected MavenSession newMavenSession(MavenProject project) {
        try {
            MavenSession session = newMavenSession();
            session.setCurrentProject(project);
            session.setProjects(Collections.singletonList(project));
            return session;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    Mojo lookupConfiguredMojo(File pom) throws Exception {
        ProjectBuildingRequest buildingRequest = newMavenSession().getProjectBuildingRequest();
        ProjectBuilder projectBuilder = lookup(ProjectBuilder.class);
        MavenProject project = projectBuilder.build(pom, buildingRequest).getProject();
        return lookupConfiguredMojo(project, "publish");
    }

    private MavenSession newMavenSession() throws MavenExecutionRequestPopulationException, ComponentLookupException, NoLocalRepositoryManagerException {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setSystemProperties(System.getProperties());
        MavenExecutionRequestPopulator requestPopulator = getContainer().lookup(MavenExecutionRequestPopulator.class);
        requestPopulator.populateDefaults(request);

        DefaultMaven maven = (DefaultMaven) getContainer().lookup(Maven.class);
        DefaultRepositorySystemSession repoSession = (DefaultRepositorySystemSession) maven.newRepositorySession(request);
        repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory().newInstance(repoSession, new LocalRepository(request.getLocalRepository().getBasedir())));
        //noinspection deprecation
        return new MavenSession(getContainer(), repoSession, request, new DefaultMavenExecutionResult());
    }
}