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

/**
 * Created by mystes-am on 29.5.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtifactIntefaceField {
    private ArtifactInterfaceInfo parent;

    public String description;
    public String path;
    public boolean optional = false;

    public ArtifactIntefaceField(String description, String path, boolean optional) {
        this.description = description;
        this.path = path;
        this.optional = optional;
    }

    public void setParent(ArtifactInterfaceInfo parent) {
        this.parent = parent;
    }

    private ArtifactInterfaceInfo getParent() {
        return this.parent;
    }

    private boolean hasParent() {
        return this.parent != null;
    }

    public String getArtifactName() {
        if (this.hasParent()) {
            return this.getParent().getArtifactName();
        }
        return null;
    }

    public boolean isDescriptionDefined() {
        return StringUtils.isNotBlank(this.description);
    }

    public boolean isPathDefined() {
        return StringUtils.isNotBlank(this.path);
    }
}
