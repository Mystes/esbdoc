package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a single Artifact. Supported artifact types are defined in the
 * Artifact.ArtifactType enum.
 */
public class Artifact implements Comparable<Artifact> {

    private final String name;
    private final String version;
    private final ArtifactType type;
    private final String path;

    public final ArtifactDescription description;

    private final String carPath;

    public Artifact(String name, String version, ArtifactType type, String path, String carPath, ArtifactDescription description) {
        if (name == null || version == null || type == null || path == null || carPath == null) {
            throw new IllegalArgumentException("All Artifact constructor parameters except description must be non-null");
        }

        this.name = name;
        this.version = version;
        this.type = type;
        this.path = path;
        this.carPath = carPath;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public ArtifactType getType() {
        return type;
    }

    /**
     * VFS path of this artifact within its car file
     *
     * @return
     */
    public String getPath() {
        return path;
    }

    /**
     * VFS path of this artifact's car file
     *
     * @return
     */
    public String getCarPath() {
        return carPath;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name: \"")
                .append(name)
                .append("\", version: \"")
                .append(version)
                .append("\", synapseType: \"")
                .append(type)
                .append("\", path: \"")
                .append(path)
                .append("\"}")
                .toString();
    }

    @Override
    public int compareTo(Artifact artifact) {
        int difference = this.type.compareTo(artifact.type);
        if (difference == 0) {
            difference = this.name.compareTo(artifact.name);
            if (difference == 0) {
                difference = this.version.compareTo(artifact.version);
            }
        }
        return difference;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Artifact) {
            Artifact other = (Artifact) o;

            return name.equals(other.name) && type.equals(other.type);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 37;

        result = 37 * result + name.hashCode();
        result = 37 * result + type.hashCode();

        return result;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArtifactDescription {

        public String purpose;
        public ArtifactInterfaceInfo receives;
        public ArtifactInterfaceInfo returns;
        public List<String> dependencies;

        public ArtifactDescription(String purpose, ArtifactInterfaceInfo receives, ArtifactInterfaceInfo returns, List<String> dependencies) {
            this.purpose = purpose;
            this.receives = receives;
            this.returns = returns;
            this.dependencies = dependencies;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArtifactInterfaceInfo {

        public String description;
        public String example;

        public List<ArtifactIntefaceField> fields;

        public void addField(ArtifactIntefaceField field) {
            if (fields == null) {
                fields = new ArrayList<ArtifactIntefaceField>();
            }
            fields.add(field);
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArtifactIntefaceField {

        public String description;
        public String path;
        public boolean optional = false;

        public ArtifactIntefaceField(String description, String path, boolean optional) {
            this.description = description;
            this.path = path;
            this.optional = optional;
        }
    }

    /**
     * Represents the different artifact types.
     */
    public enum ArtifactType {

        PROXY("synapse/proxy-service", "proxy"),
        SEQUENCE("synapse/sequence", "sequence"),
        ENDPOINT("synapse/endpoint", "endpoint"),
        API("synapse/api", "api"),
        RESOURCE("registry/resource", "resource"),
        MESSAGE_PROCESSOR("synapse/message-processors", "messageProcessor"),
        MESSAGE_STORE("synapse/message-store", "messageStore"),
        TASK("synapse/task", "task"),
        DATASERVICE("service/dataservice", "dataservice");

        private static final Map<String, ArtifactType> ARTIFACT_TYPE_MAP;

        static {
            ARTIFACT_TYPE_MAP = new HashMap<String, ArtifactType>(6);

            for (ArtifactType at : ArtifactType.values()) {
                ARTIFACT_TYPE_MAP.put(at.synapseType, at);
            }
        }

        private final String synapseType;
        private final String typeString;

        ArtifactType(String synapseType, String typeString) {
            this.synapseType = synapseType;
            this.typeString = typeString;
        }

        public static ArtifactType getArtifactTypeByTypeString(String typeString) {
            return ARTIFACT_TYPE_MAP.get(typeString);
        }

        @Override
        public String toString() {
            return typeString;
        }
    }
}
