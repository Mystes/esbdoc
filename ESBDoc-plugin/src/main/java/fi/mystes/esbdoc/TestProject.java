package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

import java.util.Set;

/**
 * Represents a single TestProject
 */
public class TestProject implements Comparable<TestProject> {

    public enum TestItemType {

        PROJECT("Project"),
        TEST_SUITE("TestSuite"),
        TEST_CASE("TestCase");

        final String prefixString;

        TestItemType(String prefixString) {
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
        return "{name: \"" + name + "\"}";
    }

    @Override
    public int compareTo(TestProject that) {
        return this.name.compareTo(that.name);
    }

    @Override
    public boolean equals(Object object) {
        if(null == object){
            return false;
        }
        if(!(object instanceof TestProject)){
            return false;
        }
        TestProject that = (TestProject) object;
        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return 37 * 37 + name.hashCode(); //TODO why like this?
    }
}