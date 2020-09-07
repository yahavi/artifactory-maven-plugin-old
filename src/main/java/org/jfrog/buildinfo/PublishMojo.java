package org.jfrog.buildinfo;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.Maven;
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
import org.jfrog.build.api.BuildInfoFields;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.build.extractor.clientConfiguration.ClientProperties;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Artifactory plugin creating and deploying JSON build data together with build artifacts.
 */
@SuppressWarnings("unused")
@Mojo(name = "publish", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class PublishMojo extends AbstractMojo {

    private static final List<String> deployGoals = Lists.newArrayList("deploy", "maven-deploy-plugin");

    @Parameter(required = true, defaultValue = "${project}")
    MavenProject project;

    @Parameter(required = true, defaultValue = "${session}")
    MavenSession session;

    @Component(role = ArtifactoryRepositoryListener.class)
    private ArtifactoryRepositoryListener repositoryListener;

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
        }
        skipDefaultDeploy();
        if (session.getGoals().stream().anyMatch(deployGoals::contains)) {
            completeConfig();
            addDeployProps();
            ArtifactoryExecutionListener executionListener = new ArtifactoryExecutionListener(session, getLog(), artifactory.delegate);
            repositoryListener.setExecutionListener(executionListener);
            session.getRequest().setExecutionListener(executionListener);
        }
    }

    private void skipDefaultDeploy() {
        // For Maven versions < 3.3.3:
        session.getExecutionProperties().setProperty("maven.deploy.skip", Boolean.TRUE.toString());
        // For Maven versions >= 3.3.3:
        session.getUserProperties().put("maven.deploy.skip", Boolean.TRUE.toString());
    }


    private void completeConfig() {
        final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        ArtifactoryClientConfiguration.BuildInfoHandler buildInfo = this.buildInfo.delegate;
        buildInfo.setBuildTimestamp(Long.toString(session.getStartTime().getTime()));
        buildInfo.setBuildStarted(DATE_FORMAT.format(session.getStartTime()));
        if (StringUtils.isBlank(buildInfo.getBuildName())) {
            buildInfo.setBuildName(project.getArtifactId());
        }
        if (StringUtils.isBlank(buildInfo.getBuildNumber())) {
            buildInfo.setBuildNumber(buildInfo.getBuildTimestamp());
        }
        buildInfo.setBuildAgentName("Maven");
        buildInfo.setBuildAgentVersion(Maven.class.getPackage().getImplementationVersion());
        if (buildInfo.getBuildRetentionDays() != null) {
            buildInfo.setBuildRetentionMinimumDate(buildInfo.getBuildRetentionDays().toString());
        }
    }

    private void addDeployProps() {
        ArtifactoryClientConfiguration.BuildInfoHandler buildInfo = this.buildInfo.delegate;
        Properties deployParams = new Properties() {{
            addDeployProp(this, BuildInfoFields.BUILD_TIMESTAMP, buildInfo.getBuildTimestamp());
            addDeployProp(this, BuildInfoFields.BUILD_NAME, buildInfo.getBuildName());
            addDeployProp(this, BuildInfoFields.BUILD_NUMBER, buildInfo.getBuildNumber());
        }};
        deployProperties.forEach((key, value) -> addDeployProp(deployParams, key, value));
        artifactory.delegate.fillFromProperties(deployParams);
    }

    private void addDeployProp(Properties deployParams, String key, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        deployParams.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX + key, value);
    }
}
