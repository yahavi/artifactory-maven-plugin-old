package org.jfrog.buildinfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.Maven;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoMavenBuilder;
import org.jfrog.build.api.builder.PromotionStatusBuilder;
import org.jfrog.build.api.release.Promotion;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import static org.jfrog.build.api.BuildInfoFields.*;


/**
 * @author Noam Y. Tenne
 */
public class BuildInfoModelPropertyResolver extends BuildInfoMavenBuilder {

    private final Log logger;

    public BuildInfoModelPropertyResolver(Log logger, ExecutionEvent event, ArtifactoryClientConfiguration clientConf) {
        super(StringUtils.firstNonBlank(clientConf.info.getBuildName(), event.getSession().getTopLevelProject().getName()));
        this.logger = logger;
        resolveCoreProperties(event, clientConf);
        resolveProperties(clientConf);
    }

    public void resolveProperties(ArtifactoryClientConfiguration clientConf) {
        artifactoryPrincipal(clientConf.publisher.getName());
        artifactoryPluginVersion(clientConf.info.getArtifactoryPluginVersion());
        principal(clientConf.info.getPrincipal());
        parentName(clientConf.info.getParentBuildName());
        parentNumber(clientConf.info.getParentBuildNumber());

        String buildUrl = clientConf.info.getBuildUrl();
        if (StringUtils.isNotBlank(buildUrl)) {
            url(buildUrl);
        }

        Vcs vcs = new Vcs();
        String vcsRevision = clientConf.info.getVcsRevision();
        if (StringUtils.isNotBlank(vcsRevision)) {
            vcs.setRevision(vcsRevision);
            vcsRevision(vcsRevision);
        }

        BuildAgent buildAgent = new BuildAgent("Maven", getMavenVersion());
        buildAgent(buildAgent);

        String agentName = StringUtils.firstNonBlank(clientConf.info.getAgentName(), buildAgent.getName());
        String agentVersion = StringUtils.firstNonBlank(clientConf.info.getAgentVersion(), buildAgent.getVersion());
        agent(new Agent(agentName, agentVersion));

        attachStagingIfNeeded(clientConf);
        artifactoryPrincipal(clientConf.publisher.getName());
        artifactoryPluginVersion(clientConf.info.getArtifactoryPluginVersion());

        for (Map.Entry<String, String> runParam : clientConf.info.getRunParameters().entrySet()) {
            MatrixParameter matrixParameter = new MatrixParameter(runParam.getKey(), runParam.getValue());
            addRunParameters(matrixParameter);
        }
    }

    private void attachStagingIfNeeded(ArtifactoryClientConfiguration clientConf) {
        if (!clientConf.info.isReleaseEnabled()) {
            return;
        }
        String stagingRepository = clientConf.publisher.getRepoKey();
        String comment = clientConf.info.getReleaseComment();
        if (comment == null) {
            comment = "";
        }
        String buildStartedIso = clientConf.info.getBuildStarted();
        Date buildStartDate;
        try {
            buildStartDate = new SimpleDateFormat(Build.STARTED_FORMAT).parse(buildStartedIso);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Build start date format error: " + buildStartedIso, e);
        }
        addStatus(new PromotionStatusBuilder(Promotion.STAGED).timestampDate(buildStartDate)
                .comment(comment).repository(stagingRepository)
                .ciUser(clientConf.info.getPrincipal()).user(clientConf.publisher.getUsername()).build());
    }

    private void resolveCoreProperties(ExecutionEvent event, ArtifactoryClientConfiguration clientConf) {
        String buildNumber = clientConf.info.getBuildNumber();
        if (StringUtils.isBlank(buildNumber)) {
            buildNumber = Long.toString(System.currentTimeMillis());
        }
        number(buildNumber);

        Date buildStartedDate = event.getSession().getRequest().getStartTime();
        String buildStarted = clientConf.info.getBuildStarted();
        if (StringUtils.isBlank(buildStarted)) {
            buildStarted = new SimpleDateFormat(Build.STARTED_FORMAT).format(buildStartedDate);
        }
        started(buildStarted);

        String buildTimestamp = clientConf.info.getBuildTimestamp();
        if (StringUtils.isBlank(buildTimestamp)) {
            buildTimestamp = Long.toString(buildStartedDate.getTime());
        }
        logResolvedProperty(BUILD_NAME, super.name);
        logResolvedProperty(BUILD_NUMBER, buildNumber);
        logResolvedProperty(BUILD_STARTED, buildStarted);
        logResolvedProperty(BUILD_TIMESTAMP, buildTimestamp);
    }

    private String getMavenVersion() {
        // Get Maven version from this class
        Properties mavenVersionProperties = new Properties();
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("org/apache/maven/messages/build.properties")) {
            if (inputStream != null) {
                mavenVersionProperties.load(inputStream);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error while extracting Maven version properties from: org/apache/maven/messages/build.properties", e);
        }

        // Get Maven version from Maven core class
        if (mavenVersionProperties.isEmpty()) {
            try (InputStream inputStream = Maven.class.getClassLoader().getResourceAsStream("META-INF/maven/org.apache.maven/maven-core/pom.properties")) {
                if (inputStream != null) {
                    mavenVersionProperties.load(inputStream);
                }
            } catch (IOException e) {
                throw new RuntimeException("Error while extracting Maven version properties from: META-INF/maven/org.apache.maven/maven-core/pom.properties", e);
            }
        }

        if (mavenVersionProperties.isEmpty()) {
            throw new RuntimeException("Could not extract Maven version: unable to find resources 'org/apache/maven/messages/build.properties' or 'META-INF/maven/org.apache.maven/maven-core/pom.properties'");
        }
        String version = mavenVersionProperties.getProperty("version");
        if (StringUtils.isBlank(version)) {
            throw new RuntimeException("Could not extract Maven version: no version property found in the resource 'org/apache/maven/messages/build.properties' or or 'META-INF/maven/org.apache.maven/maven-core/pom.properties'");
        }
        return version;
    }

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Build Info Model Property Resolver: " + key + " = " + value);
    }
}