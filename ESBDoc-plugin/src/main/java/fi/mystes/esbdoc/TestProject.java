package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

import java.util.Set;

/**
 * Represents a single TestProject
 */
public class TestProject implements Comparable<TestProject> {

    public enum PropertyType {

        PROJECT("Project"),
        TEST_SUITE("TestSuite"),
        TEST_CASE("TestCase");

        final String prefixString;

        public String getPrefixString() {
            return prefixString;
        }

        PropertyType(String prefixString) {
            this.prefixString = prefixString;
        }

        @Override
        public String toString() {
            return prefixString;
        }
    }

    private final String name;
    private final String filename;
    private final Set<TestSuite> suites;

    public TestProject(String name, String filename, Set<TestSuite> suites) {
        if (name == null || filename == null || suites == null) {
            throw new IllegalArgumentException("All TestProject constructor parameters must be non-null");
        }

        this.name = name;
        this.filename = filename;
        this.suites = suites;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public Set<TestSuite> getTestSuites() {
        return suites;
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("{name: \"")
                .append(name)
                .append("\"}")
                .toString();
    }

    @Override
    public int compareTo(TestProject project) {
        int difference = this.name.compareTo(project.name);
        return difference;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof TestProject) {
            TestProject other = (TestProject) o;

            return name.equals(other.name);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 37;

        result = 37 * result + name.hashCode();

        return result;
    }
}