package org.jfrog.buildinfo;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;

import java.util.List;

/**
 * @author yahavi
 */
@Component(role = AbstractMavenLifecycleParticipant.class)
public class ArtifactoryMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

    @Requirement
    ArtifactoryEclipseResolversHelper helper;

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        super.afterProjectsRead(session);
        MavenProject project = session.getTopLevelProject();
        List<ArtifactRepository> resolutionRepositories = helper.getResolutionRepositories();
        project.setPluginArtifactRepositories(resolutionRepositories);
        project.setRemoteArtifactRepositories(resolutionRepositories);
        System.out.println("afterProjectsRead!!!");
    }

    @Override
    public void afterSessionStart(MavenSession session) throws MavenExecutionException {
        super.afterSessionStart(session);
        System.out.println("afterSessionStart!!!");

    }

    @Override
    public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
        super.afterSessionEnd(session);
        System.out.println("afterSessionEnd!!!");

    }
}
