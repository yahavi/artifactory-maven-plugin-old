package org.jfrog.buildinfo;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.DefaultExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Requirement;

import java.util.List;
import java.util.Map;

/**
 * Artifactory plugin creating and deploying JSON build data together with build artifacts.
 */
@SuppressWarnings("unused")
@Mojo(name = "publish", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class PublishMojo extends AbstractMojo {

    @Parameter(required = true, defaultValue = "${project}")
    MavenProject project;

    @Parameter(required = true, defaultValue = "${session}")
    MavenSession session;

    @Component(role = ExecutionEventCatapult.class)
    private DefaultExecutionEventCatapult eventCatapult;

    @Requirement
    ArtifactoryRepoHelper resolversHelper;

    @Parameter
    Map<String, String> deployProperties;

    @Parameter
    Config.Artifactory artifactory;

    @Parameter
    Config.Publisher publisher;

    @Parameter
    Config.BuildInfo buildInfo;

    @Parameter
    Config.Resolver resolver;

    public void execute() {
        ArtifactoryRepoHelper helper = new ArtifactoryRepoHelper(getLog(), session, artifactory.delegate);
        List<ArtifactRepository> resolutionRepositories = helper.getResolutionRepositories();
        for (MavenProject mavenProject : session.getProjects()) {
            mavenProject.setPluginArtifactRepositories(resolutionRepositories);
            mavenProject.setRemoteArtifactRepositories(resolutionRepositories);
            mavenProject.setReleaseArtifactRepository(helper.getReleaseRepository());
            mavenProject.setSnapshotArtifactRepository(helper.getSnapshotRepository());
        }
        session.getRequest().setExecutionListener(new ArtifactoryExecutionListener(getLog(), artifactory.delegate));
    }
}
