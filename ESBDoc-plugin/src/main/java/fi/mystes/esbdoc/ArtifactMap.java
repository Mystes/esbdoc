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

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.TreeMap;

/**
 * Created by mystes-am on 29.5.2015.
 */
public class ArtifactMap extends TreeMap<String, Artifact> {
    public Collection<Artifact> getArtifacts(){
        return this.values();
    }

    public boolean addValid(Artifact artifact){
        if(null == artifact){
            return false;
        }
        if(StringUtils.isBlank(artifact.getName())){
            return false;
        }
        this.put(artifact.getName(), artifact);
        return true;
    }
}
