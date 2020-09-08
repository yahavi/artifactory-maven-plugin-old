package org.jfrog.buildinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import junit.framework.TestCase;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;
import org.jfrog.build.api.Build;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.RequestDefinition;

import java.io.File;
import java.io.IOException;

import static org.mockserver.model.HttpRequest.request;

/**
 * == Integration tests ==
 * The tests execute 'mvn clean deploy' automatically on each one of the test projects.
 * To run the integration tests, execute 'mvn clean verify -DskipITs=false'
 * To remote debug the integration tests add the following code before 'verifier.executeGoals':
 * verifier.setEnvironmentVariable("MAVEN_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
 *
 * @author yahavi
 */
public class ArtifactoryPluginITest extends TestCase {

    public void testMultiModule() throws Exception {
        try (ClientAndServer mockServer = ClientAndServer.startClientAndServer(8081)) {
            initializeMockServer(mockServer);
            executeGoals("multi-example");
            Build build = getBuild(mockServer);
            assertEquals("buildName", build.getName());
            System.out.println(build);
        }
    }

    private void initializeMockServer(ClientAndServer mockServer) {
        mockServer.when(request("/api/system/version")).respond(HttpResponse.response("{\"version\":\"7.0.0\"}"));
        mockServer.when(request()).respond(HttpResponse.response().withStatusCode(200));
    }

    private void executeGoals(String projectName) throws VerificationException, IOException {
        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/integration/" + projectName);
        Verifier verifier = new Verifier(testDir.getAbsolutePath());
        verifier.executeGoals(Lists.newArrayList("clean", "deploy"));
        verifier.verifyErrorFreeLog();
    }

    private Build getBuild(ClientAndServer mockServer) throws JsonProcessingException {
        RequestDefinition[] requestDefinitions = mockServer.retrieveRecordedRequests(request("/api/build"));
        assertEquals(1, ArrayUtils.getLength(requestDefinitions));
        RequestDefinition buildInfoRequest = requestDefinitions[0];
        ObjectMapper mapper = new ObjectMapper();
        JsonNode buildInfoRequestNode = mapper.readTree(buildInfoRequest.toString());
        System.out.println(buildInfoRequestNode);
        JsonNode body = buildInfoRequestNode.get("body");
        JsonNode json = body.get("json");
        return mapper.readValue(json.toString(), Build.class);
    }
}
