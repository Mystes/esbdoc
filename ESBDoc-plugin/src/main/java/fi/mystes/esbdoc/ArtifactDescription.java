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

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Created by mystes-am on 29.5.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtifactDescription {
    private Artifact parent;

    public String purpose;
    public ArtifactInterfaceInfo receives;
    public ArtifactInterfaceInfo returns;
    public List<String> dependencies;

    private ArtifactDescription(String purpose, ArtifactInterfaceInfo receives, ArtifactInterfaceInfo returns, List<String> dependencies) {
        this.purpose = purpose;
        this.receives = receives;
        this.returns = returns;
        this.dependencies = dependencies;
    }

    public static ArtifactDescription with(String purpose, ArtifactInterfaceInfo receives, ArtifactInterfaceInfo returns, List<String> dependencies) {
        ArtifactDescription artifactDescription = new ArtifactDescription(purpose, receives, returns, dependencies);
        if (null != receives) {
            receives.setParent(artifactDescription);
        }
        if (null != returns) {
            returns.setParent(artifactDescription);
        }
        return artifactDescription;
    }
    
    public static ArtifactDescription withNoDescription() {
    	return with(null, null, null, null);
    }

    public void setParent(Artifact parent) {
        this.parent = parent;
    }

    private Artifact getParent() {
        return this.parent;
    }

    private boolean hasParent() {
        return this.parent != null;
    }

    public boolean isPurposeDefined() {
        return StringUtils.isNotBlank(this.purpose);
    }

    public boolean isReceivesDefined() {
        return null != this.receives;
    }

    public boolean isReturnsDefined() {
        return null != this.returns;
    }

    public boolean isDependenciesDefined() {
        if (null == dependencies) {
            return false;
        }
        return !dependencies.isEmpty();
    }

    public String getArtifactName() {
        if (this.hasParent()) {
            return this.getParent().getName();
        }
        return null;
    }
}
