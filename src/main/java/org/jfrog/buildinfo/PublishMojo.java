package org.jfrog.buildinfo;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.DefaultExecutionEventCatapult;
import org.apache.maven.lifecycle.internal.ExecutionEventCatapult;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.maven.BuildInfoRecorder;
import org.jfrog.build.extractor.maven.BuildInfoRecorderLifecycleParticipant;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Artifactory plugin creating and deploying JSON build data together with build artifacts.
 */
@Mojo(name = "publish", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class PublishMojo extends AbstractMojo {
    /**
     * ---------------------------
     * Container-injected objects
     * ---------------------------
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(required = true, defaultValue = "${session}")
    private MavenSession session;

    @Component(role = AbstractMavenLifecycleParticipant.class)
    private BuildInfoRecorderLifecycleParticipant listener;

    @Component(role = ExecutionEventCatapult.class)
    private DefaultExecutionEventCatapult eventCatapult;

    /**
     * ----------------
     * Mojo parameters
     * ----------------
     */

    @Parameter
    File propertiesFile;

    @Parameter
    String properties;

    @Parameter
    String deployGoals = "deploy,maven-deploy-plugin";

    @Parameter
    boolean pomPropertiesPriority = false;

    @Parameter
    Map<String, String> deployProperties = new HashMap<String, String>();

    /**
     * ----------------
     * Mojo parameters - property handlers
     * ----------------
     */

//    @Parameter
//    ArtifactoryClientConfiguration artifactory;// = new Config.Artifactory();
//
//    @Parameter
//    Config.Resolver resolver = new Config.Resolver();
//
//    @Parameter
//    Config.Publisher publisher = new Config.Publisher();
//
//    @Parameter
//    Config.BuildInfo buildInfo = new Config.BuildInfo();
//
//    @Parameter
//    Config.LicenseControl licenses = new Config.LicenseControl();
//
//    @Parameter
//    Config.IssuesTracker issues = new Config.IssuesTracker();

    /**
     * Helper object
     */
    private PublishMojoHelper helper;

    public void execute() throws MojoExecutionException {
        getLog().info("hello!!!!!!!!!!!!!!!!!!!!!!!!");
        // Cannot use instanceof because of classLoader issues
//        boolean invokedAlready = session.getRequest().getExecutionListener().getClass().getCanonicalName().equals(BuildInfoRecorder.class.getCanonicalName());
//
//        if (invokedAlready) {
//            return;
//        }

        helper = new PublishMojoHelper(this);
        if (getLog().isDebugEnabled()) {
//            helper.printConfigurations();
        }
//
//        skipDefaultDeploy()
//        completeConfig()
//        helper.createPropertiesFile()
//        recordBuildInfo()
    }
}
