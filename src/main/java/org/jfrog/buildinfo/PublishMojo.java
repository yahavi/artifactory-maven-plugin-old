package org.jfrog.buildinfo;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
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

import static org.jfrog.buildinfo.Utils.getMavenVersion;

/**
 * Artifactory plugin creating and deploying JSON build data together with build artifacts.
 */
@SuppressWarnings("unused")
@Mojo(name = "publish", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class PublishMojo extends AbstractMojo {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final String[] DEPLOY_GOALS = {"deploy", "maven-deploy-plugin"};

    @Parameter(required = true, defaultValue = "${project}")
    MavenProject project;

    @Parameter(required = true, defaultValue = "${session}")
    MavenSession session;

    @Component(role = ArtifactoryRepositoryListener.class)
    private ArtifactoryRepositoryListener repositoryListener;

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
        if (session.getGoals().stream().anyMatch(goal -> ArrayUtils.contains(DEPLOY_GOALS, goal))) {
            completeConfig();
            addDeployProperties();
            ArtifactoryExecutionListener executionListener = new ArtifactoryExecutionListener(session, getLog(), artifactory.delegate);
            repositoryListener.setExecutionListener(executionListener);
            session.getRequest().setExecutionListener(executionListener);
        }
    }

    @SuppressWarnings("deprecation")
    private void skipDefaultDeploy() {
        // For Maven versions < 3.3.3:
        session.getExecutionProperties().setProperty("maven.deploy.skip", Boolean.TRUE.toString());
        // For Maven versions >= 3.3.3:
        session.getUserProperties().put("maven.deploy.skip", Boolean.TRUE.toString());
    }

    private void completeConfig() {
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
        buildInfo.setBuildAgentVersion(getMavenVersion(getClass()));
        if (buildInfo.getBuildRetentionDays() != null) {
            buildInfo.setBuildRetentionMinimumDate(buildInfo.getBuildRetentionDays().toString());
        }
    }

    private void addDeployProperties() {
        ArtifactoryClientConfiguration.BuildInfoHandler buildInfo = this.buildInfo.delegate;
        Properties deployProperties = new Properties() {{
            addDeployProperty(this, BuildInfoFields.BUILD_TIMESTAMP, buildInfo.getBuildTimestamp());
            addDeployProperty(this, BuildInfoFields.BUILD_NAME, buildInfo.getBuildName());
            addDeployProperty(this, BuildInfoFields.BUILD_NUMBER, buildInfo.getBuildNumber());
        }};
        this.deployProperties.forEach((key, value) -> addDeployProperty(deployProperties, key, value));
        artifactory.delegate.fillFromProperties(deployProperties);
    }

    private void addDeployProperty(Properties deployProperties, String key, String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        deployProperties.put(ClientProperties.PROP_DEPLOY_PARAM_PROP_PREFIX + key, value);
    }
}
