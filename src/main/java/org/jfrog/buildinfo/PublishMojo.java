package org.jfrog.buildinfo;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Artifactory plugin creating and deploying JSON build data together with build artifacts.
 */
@Mojo(name = "publish", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class PublishMojo extends AbstractMojo {

    @Parameter(required = true, defaultValue = "${project}")
    private MavenProject project;

    @Parameter(required = true, defaultValue = "${session}")
    private MavenSession session;

    @Parameter
    Config.Artifactory artifactory;

    @Parameter
    Config.Resolver resolver;

    @Parameter
    Config.Publisher publisher;

    @Parameter
    Config.BuildInfo buildInfo;

    @Parameter
    Config.LicenseControl licenses;

    @Parameter
    Config.IssuesTracker issues;

    public void execute() throws MojoExecutionException {
        getLog().info("hello!!!!!!!!!!!!!!!!!!!!!!!!");

    }
}
