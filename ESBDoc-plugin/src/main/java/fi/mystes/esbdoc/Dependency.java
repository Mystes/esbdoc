package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

/**
 * Represents the different dependency types between artifacts
 */
public class Dependency implements Comparable<Dependency> {

    public final Artifact dependent;
    public final Object dependency;
    private final DependencyType type;

    public Dependency(Artifact dependent, Object dependency, DependencyType dependencyType) {
        if (dependent == null || dependency == null || dependencyType == null) {
            throw new IllegalArgumentException("All Dependency constructor parameters must be non-null");
        }
        this.dependent = dependent;
        this.dependency = dependency;
        this.type = dependencyType;
    }

    public Object getDependency() {
        return dependency;
    }

    public DependencyType getType() {
        return type;
    }

    @Override
    public int compareTo(Dependency that) {
        if(null == that){ return -1; }

        boolean bothAreArtifacts = this.dependency instanceof Artifact && that.dependency instanceof Artifact;
        boolean bothAreStrings = this.dependency instanceof String && that.dependency instanceof String;

        if (bothAreArtifacts || bothAreStrings) {
            return ((Comparable) this.dependency).compareTo((Comparable) that.dependency);
        }

        if (this.dependency instanceof Artifact) {
            return -1;
        }

        return 1;
    }

    @Override
    public boolean equals(Object object) {
        if(null == object){ return false; }
        if(!(object instanceof Dependency)){return false; }

        Dependency other = (Dependency) object;
        return dependent.equals(other.dependent) && dependency.equals(other.dependency) && type.equals(other.type);
    }

    @Override
    public int hashCode() {
        int result = 37;
        result = 37 * result + dependent.hashCode();
        result = 37 * result + dependency.hashCode();
        result = 37 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        String dependencyString = dependency instanceof Artifact ? ((Artifact) dependency).getName() : (String) dependency;
        return dependent.getName() + " -> " + dependencyString + " :[" + type + "]";
    }
}
