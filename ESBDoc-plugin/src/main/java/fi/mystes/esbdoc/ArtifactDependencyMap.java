/**
 * Copyright 2018 Mystes Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
