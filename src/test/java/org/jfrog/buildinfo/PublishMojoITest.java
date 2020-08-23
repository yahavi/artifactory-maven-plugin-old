package org.jfrog.buildinfo;

import java.io.File;

/**
 * @author yahavi
 */

public class PublishMojoITest extends ArtifactoryPluginTestCase {

    public void testSomething() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/maven-example/pom.xml");
        PublishMojo mojo = (PublishMojo) lookupConfiguredMojo(testPom);
        assertNotNull(mojo);
        mojo.artifactory = new Config.Artifactory();
        mojo.execute();
    }

}
