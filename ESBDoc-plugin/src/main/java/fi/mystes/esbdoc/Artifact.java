package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

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

    private Artifact(String name, String version, ArtifactType type, String path, String carPath, ArtifactDescription description) {
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

    public static Artifact with(String name, String version, ArtifactType type, String path, String carPath, ArtifactDescription description){
        Artifact artifact = new Artifact(name, version, type, path, carPath, description);
        if(null != description) {
            description.setParent(artifact);
        }
        return artifact;
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

    public boolean isDescriptionDefined(){
        return null != this.description;
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
    public int compareTo(Artifact that) {
        int difference = this.type.compareTo(that.type);
        if(0 != difference){
            return difference;
        }

        difference = this.name.compareTo(that.name);
        if(0 != difference){
            return difference;
        }

        return this.version.compareTo(that.version);
    }

    @Override
    public boolean equals(Object object) {
        if(null == object){
            return false;
        }
        if(!(object instanceof Artifact)){
            return false;
        }

        Artifact that = (Artifact) object;
        boolean namesEqual = this.name.equals(that.name);
        boolean typesEqual = this.type.equals(that.type);
        return namesEqual && typesEqual;
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

        public static ArtifactDescription with(String purpose, ArtifactInterfaceInfo receives, ArtifactInterfaceInfo returns, List<String> dependencies){
            ArtifactDescription artifactDescription = new ArtifactDescription(purpose, receives, returns, dependencies);
            if(null != receives) {
                receives.setParent(artifactDescription);
            }
            if(null != returns) {
                returns.setParent(artifactDescription);
            }
            return artifactDescription;
        }

        private void setParent(Artifact parent){
            this.parent = parent;
        }

        private Artifact getParent(){
            return this.parent;
        }

        private boolean hasParent(){
            return this.parent != null;
        }

        public boolean isPurposeDefined(){
            return StringUtils.isNotBlank(this.purpose);
        }

        public boolean isReceivesDefined(){
            return null != this.receives;
        }

        public boolean isReturnsDefined(){
            return null != this.returns;
        }

        public boolean isDependenciesDefined(){
            if(null == dependencies){
                return false;
            }
            return !dependencies.isEmpty();
        }

        private String getArtifactName(){
            if(this.hasParent()){
                return this.getParent().getName();
            }
            return null;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArtifactInterfaceInfo {
        private ArtifactDescription parent;

        public String description;
        public String example;

        public List<ArtifactIntefaceField> fields;

        public void addField(ArtifactIntefaceField field) {
            if (fields == null) {
                fields = new ArrayList<ArtifactIntefaceField>();
            }
            if(null != field) {
                field.setParent(this);
                fields.add(field);
            }
        }

        private void setParent(ArtifactDescription parent){
            this.parent = parent;
        }

        private ArtifactDescription getParent(){
            return this.parent;
        }

        private boolean hasParent(){
            return this.parent != null;
        }

        private String getArtifactName(){
            if(this.hasParent()){
                return this.getParent().getArtifactName();
            }
            return null;
        }

        public boolean isDescriptionDefined(){
            return StringUtils.isNotBlank(this.description);
        }

        public boolean isExampleDefined(){
            return StringUtils.isNotBlank(this.example);
        }

        public boolean isFieldsDefined(){
            if(null == fields){
                return false;
            }
            return !fields.isEmpty();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArtifactIntefaceField {
        private ArtifactInterfaceInfo parent;

        public String description;
        public String path;
        public boolean optional = false;

        public ArtifactIntefaceField(String description, String path, boolean optional) {
            this.description = description;
            this.path = path;
            this.optional = optional;
        }

        private void setParent(ArtifactInterfaceInfo parent){
            this.parent = parent;
        }

        private ArtifactInterfaceInfo getParent(){
            return this.parent;
        }

        private boolean hasParent(){
            return this.parent != null;
        }

        public String getArtifactName(){
            if(this.hasParent()){
                return this.getParent().getArtifactName();
            }
            return null;
        }
    }

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

        private static final Map<String, ArtifactType> artifactTypeMap;

        static {
            artifactTypeMap = new HashMap<String, ArtifactType>(6); //TODO why that 6 right there?

            for (ArtifactType at : ArtifactType.values()) {
                artifactTypeMap.put(at.synapseType, at);
            }
        }

        private final String synapseType;
        private final String typeString;

        ArtifactType(String synapseType, String typeString) {
            this.synapseType = synapseType;
            this.typeString = typeString;
        }

        public static ArtifactType correspondingTo(String typeString) {
            return artifactTypeMap.get(typeString);
        }

        @Override
        public String toString() {
            return typeString;
        }
    }
}
