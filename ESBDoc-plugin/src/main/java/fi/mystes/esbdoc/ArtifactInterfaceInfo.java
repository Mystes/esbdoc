package fi.mystes.esbdoc;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mystes-am on 29.5.2015.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArtifactInterfaceInfo {
    private Artifact.ArtifactDescription parent;

    public String description;
    public String example;

    public List<ArtifactIntefaceField> fields;

    public void addField(ArtifactIntefaceField field) {
        if (fields == null) {
            fields = new ArrayList<ArtifactIntefaceField>();
        }
        if (null != field) {
            field.setParent(this);
            fields.add(field);
        }
    }

    public void setParent(Artifact.ArtifactDescription parent) {
        this.parent = parent;
    }

    private Artifact.ArtifactDescription getParent() {
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

    public boolean isExampleDefined() {
        return StringUtils.isNotBlank(this.example);
    }

    public boolean isFieldsDefined() {
        if (null == fields) {
            return false;
        }
        return !fields.isEmpty();
    }
}
