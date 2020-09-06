package org.jfrog.buildinfo.types;

import org.apache.maven.artifact.Artifact;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yahavi
 */
public class ModuleArtifacts extends ThreadLocal<Set<Artifact>> {

    public Set<Artifact> getOrCreate() {
        Set<Artifact> artifacts = super.get();
        if (artifacts == null) {
            artifacts = new HashSet<>();
            set(artifacts);
        }
        return artifacts;
    }

    public void addAll(Collection<Artifact> artifactsToAdd) {
        Set<Artifact> artifacts = getOrCreate();
        artifacts.addAll(artifactsToAdd);
    }

    public void add(Artifact artifactToAdd) {
        Set<Artifact> artifacts = getOrCreate();
        artifacts.add(artifactToAdd);
    }
}
