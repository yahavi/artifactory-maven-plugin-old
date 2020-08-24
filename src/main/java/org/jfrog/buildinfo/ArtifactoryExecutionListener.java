package org.jfrog.buildinfo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.AbstractExecutionListener;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.jfrog.build.api.builder.BuildInfoMavenBuilder;
import org.jfrog.build.api.builder.ModuleBuilder;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.jfrog.build.extractor.BuildInfoExtractorUtils.getModuleIdString;

/**
 * @author yahavi
 */
public class ArtifactoryExecutionListener extends AbstractExecutionListener {

    private final ThreadLocal<Set<Artifact>> currentModuleDependencies = ThreadLocal.withInitial(() -> Collections.synchronizedSet(new HashSet<>()));
    private final ThreadLocal<Set<Artifact>> currentModuleArtifacts = ThreadLocal.withInitial(() -> Collections.synchronizedSet(new HashSet<>()));
    private final ThreadLocal<ModuleBuilder> currentModule = ThreadLocal.withInitial(ModuleBuilder::new);
    private final Set<Artifact> resolvedArtifacts = Collections.synchronizedSet(new HashSet<>());
    private BuildInfoMavenBuilder buildInfoBuilder;

    private final Log logger;

    public ArtifactoryExecutionListener(Log logger) {
        this.logger = logger;
    }

    /**
     * Initialize the buildInfoBuilder.
     *
     * @param event - The execution event
     */
    @Override
    public void sessionStarted(ExecutionEvent event) {
        System.out.println(event);
    }

    /**
     * Initialize current module
     *
     * @param event - The execution event
     */
    @Override
    public void projectStarted(ExecutionEvent event) {
        MavenProject project = event.getProject();
        if (project == null) {
            logger.warn("Skipping Artifactory Build-Info module initialization: Null project.");
            return;
        }

        ModuleBuilder module = new ModuleBuilder();
        module.id(getModuleIdString(project.getGroupId(), project.getArtifactId(), project.getVersion()));
        module.properties(project.getProperties());

        currentModule.set(module);
    }

    /**
     * Finalize module - populate 'currentModuleArtifacts' and 'currentModuleDependencies'.
     *
     * @param event - The execution event
     */
    @Override
    public void projectSucceeded(ExecutionEvent event) {
        MavenProject project = event.getProject();
        Set<Artifact> artifacts = currentModuleArtifacts.get();

        // Add artifacts
        artifacts.add(project.getArtifact());
        artifacts.addAll(project.getAttachedArtifacts());
    }

    /**
     * Build and publish build info
     * @param event - The execution event
     */
    @Override
    public void sessionEnded(ExecutionEvent event) {
        System.out.println(event);
    }

}
