package org.jfrog.buildinfo;

import junit.framework.TestCase;
import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yahavi
 */
public class ArtifactoryMavenPluginITest extends TestCase {
    public void testMyPlugin() throws Exception {

//        File testDir = ResourceExtractor.simpleExtractResources(getClass(), "/maven-example");
//
//        Verifier verifier = new Verifier(testDir.getAbsolutePath());
////        Map<String, String> envVars = new HashMap<String, String>() {{
////            put("MAVEN_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
////        }};
//        verifier.executeGoal("install");
//        verifier.verifyErrorFreeLog();
    }
}
