package org.jfrog.buildinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.*;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.NoLocalRepositoryManagerException;
import org.jfrog.buildinfo.resolution.ArtifactoryRepositoryListener;
import org.jfrog.buildinfo.types.MavenLogger;
import org.jfrog.buildinfo.types.PlexusLogger;
import org.junit.Before;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * @author yahavi
 */
public abstract class PublishMojoTestBase extends AbstractMojoTestCase {

    private final File testPom = new File(getBasedir(), "src/test/resources/unit-tests-pom/pom.xml");
    PublishMojo mojo;

    static Date TEST_DATE;

    static {
        try {
            TEST_DATE = new SimpleDateFormat("dd/MM/yyyy").parse("01/01/2020");
        } catch (ParseException e) {
            // Ignore
        }
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mojo = createPublishMojo(testPom);
        assertNotNull(mojo);
        mojo.execute();
    }

    @Override
    protected String getPluginDescriptorLocation() {
        return "META-INF/maven/org.jfrog.buildinfo/artifactory-maven-plugin/plugin-help.xml";
    }

    @Override
    protected MavenSession newMavenSession(MavenProject project) {
        try {
            MavenSession session = newMavenSession();
            session.setCurrentProject(project);
            session.setProjects(Collections.singletonList(project));
            session.getGoals().add("deploy");
            return session;
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    PublishMojo createPublishMojo(File pom) throws Exception {
        ProjectBuildingRequest buildingRequest = newMavenSession().getProjectBuildingRequest();
        ProjectBuilder projectBuilder = lookup(ProjectBuilder.class);
        MavenProject project = projectBuilder.build(pom, buildingRequest).getProject();
        PluginExecution execution = project.getPlugin("org.apache.maven.plugins:artifactory-maven-plugin").getExecutions().get(0);
        Xpp3Dom dom = (Xpp3Dom) execution.getConfiguration();
        PublishMojo mojo = (PublishMojo) lookupConfiguredMojo(project, "publish");
        fillMojoFromConfiguration(mojo, dom);
        return mojo;
    }

    private MavenSession newMavenSession() throws MavenExecutionRequestPopulationException, ComponentLookupException, NoLocalRepositoryManagerException {
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setSystemProperties(System.getProperties());
        request.setStartTime(TEST_DATE);
        MavenExecutionRequestPopulator requestPopulator = getContainer().lookup(MavenExecutionRequestPopulator.class);
        requestPopulator.populateDefaults(request);

        DefaultMaven maven = (DefaultMaven) getContainer().lookup(Maven.class);
        DefaultRepositorySystemSession repoSession = (DefaultRepositorySystemSession) maven.newRepositorySession(request);
        repoSession.setLocalRepositoryManager(new SimpleLocalRepositoryManagerFactory().newInstance(repoSession, new LocalRepository(request.getLocalRepository().getBasedir())));
        //noinspection deprecation
        return new MavenSession(getContainer(), repoSession, request, new DefaultMavenExecutionResult());
    }

    private void fillMojoFromConfiguration(PublishMojo mojo, Xpp3Dom configuration) throws JsonProcessingException {
        ObjectMapper objectMapper = new XmlMapper().registerModule(new GuavaModule());
        mojo.deployProperties = objectMapper.readValue(configuration.getChild("deployProperties").toString(), new TypeReference<Map<String, String>>() {
        });
        mojo.artifactory = objectMapper.readValue(configuration.getChild("artifactory").toString(), Config.Artifactory.class);
        mojo.buildInfo = objectMapper.readValue(configuration.getChild("buildInfo").toString(), Config.BuildInfo.class);
        mojo.publisher = objectMapper.readValue(configuration.getChild("publisher").toString(), Config.Publisher.class);
        mojo.resolver = objectMapper.readValue(configuration.getChild("resolver").toString(), Config.Resolver.class);
        Log log = new MavenLogger();
        mojo.setLog(log);
        mojo.repositoryListener = new ArtifactoryRepositoryListener(new PlexusLogger(log));
    }
}