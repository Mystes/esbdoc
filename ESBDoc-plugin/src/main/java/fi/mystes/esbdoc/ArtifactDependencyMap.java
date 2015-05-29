package fi.mystes.esbdoc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Created by mystes-am on 29.5.2015.
 */
public class ArtifactDependencyMap extends TreeMap<Artifact, Set<Dependency>> {

    public interface DependencySet extends Set<Dependency> {
    }

    public interface ArtifactDependency extends Entry<Artifact, Set<Dependency>> {

    }

    public List<String> toDependencyStrings(){
        List<String> dependencyStrings = new ArrayList<String>();
        for(Entry<Artifact, Set<Dependency>> entry : entrySet()){
            for (Dependency dependency : entry.getValue()) {
                dependencyStrings.add(dependency.toString());
            }
        }
        return dependencyStrings;
    }

}
