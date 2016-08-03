package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
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
        return null != this.description && null != this.description.purpose;
    }

    public String getPath() {
        return path;
    }

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

}
