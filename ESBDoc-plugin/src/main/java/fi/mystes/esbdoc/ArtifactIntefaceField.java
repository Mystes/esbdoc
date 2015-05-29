package fi.mystes.esbdoc;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by mystes-am on 29.5.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtifactIntefaceField {
    private Artifact.ArtifactInterfaceInfo parent;

    public String description;
    public String path;
    public boolean optional = false;

    public ArtifactIntefaceField(String description, String path, boolean optional) {
        this.description = description;
        this.path = path;
        this.optional = optional;
    }

    public void setParent(Artifact.ArtifactInterfaceInfo parent) {
        this.parent = parent;
    }

    private Artifact.ArtifactInterfaceInfo getParent() {
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
