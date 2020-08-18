package org.jfrog.buildinfo;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.junit.Test;

import java.io.File;

/**
 * @author yahavi
 */

public class PublishMojoITest extends AbstractMojoTestCase {

    /**
     * @throws Exception if any
     */
    @Test
    public void testSomething()
            throws Exception {
        File pom = getTestFile("src/test/resources/maven-example/pom.xml");
        assertNotNull(pom);
        assertTrue(pom.exists());

        PublishMojo myMojo = (PublishMojo) lookupMojo("publish", pom);
        assertNotNull(myMojo);
        myMojo.execute();

    }

}
