package org.jfrog.buildinfo;

import org.apache.maven.plugin.logging.Log;
import org.jfrog.build.extractor.clientConfiguration.ArtifactoryClientConfiguration;
import org.jfrog.buildinfo.deployment.BuildInfoClientBuilder;
import org.jfrog.buildinfo.types.MavenLogger;
import org.jfrog.buildinfo.utils.MavenBuildInfoLogger;
import org.junit.Before;

/**
 * @author yahavi
 */
public class BuildInfoClientBuilderTest extends PublishMojoTestBase {

    private final Log log = new MavenLogger();
    private BuildInfoClientBuilder buildInfoClientBuilder;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.buildInfoClientBuilder = new BuildInfoClientBuilder(new MavenLogger());
    }

    public void clientPropsTest() {
        ArtifactoryClientConfiguration configuration = new ArtifactoryClientConfiguration(new MavenBuildInfoLogger(log));

    }
}
