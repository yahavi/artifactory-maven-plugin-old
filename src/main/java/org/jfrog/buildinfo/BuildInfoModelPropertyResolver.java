package org.jfrog.buildinfo;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.api.*;
import org.jfrog.build.api.builder.BuildInfoMavenBuilder;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.jfrog.build.api.BuildInfoFields.*;
import static org.jfrog.buildinfo.Utils.getMavenVersion;


/**
 * @author Noam Y. Tenne
 */
public class BuildInfoModelPropertyResolver extends BuildInfoMavenBuilder {

    private final Log logger;

    public BuildInfoModelPropertyResolver(Log logger, MavenSession session, ArtifactoryClientConfiguration clientConf) {
        super(StringUtils.firstNonBlank(clientConf.info.getBuildName(), session.getTopLevelProject().getName()));
        this.logger = logger;
        resolveCoreProperties(session, clientConf);
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

        BuildAgent buildAgent = new BuildAgent("Maven", getMavenVersion(getClass()));
        buildAgent(buildAgent);

        String agentName = StringUtils.firstNonBlank(clientConf.info.getAgentName(), buildAgent.getName());
        String agentVersion = StringUtils.firstNonBlank(clientConf.info.getAgentVersion(), buildAgent.getVersion());
        agent(new Agent(agentName, agentVersion));

        artifactoryPrincipal(clientConf.publisher.getName());
        artifactoryPluginVersion(clientConf.info.getArtifactoryPluginVersion());

        for (Map.Entry<String, String> runParam : clientConf.info.getRunParameters().entrySet()) {
            MatrixParameter matrixParameter = new MatrixParameter(runParam.getKey(), runParam.getValue());
            addRunParameters(matrixParameter);
        }
    }

    private void resolveCoreProperties(MavenSession session, ArtifactoryClientConfiguration clientConf) {
        String buildNumber = clientConf.info.getBuildNumber();
        if (StringUtils.isBlank(buildNumber)) {
            buildNumber = Long.toString(System.currentTimeMillis());
        }
        number(buildNumber);

        Date buildStartedDate = session.getRequest().getStartTime();
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

    private void logResolvedProperty(String key, String value) {
        logger.debug("Artifactory Build Info Model Property Resolver: " + key + " = " + value);
    }
}