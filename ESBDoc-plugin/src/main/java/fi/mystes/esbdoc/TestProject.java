package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

import java.util.Set;

/**
 * Represents a single TestProject
 */
public class TestProject implements Comparable<TestProject> {

    private final String name;
    private final String filename;
    private final Set<TestSuite> suites;

    public TestProject(String name, String filename, Set<TestSuite> suites) {
        assertNotNull("name", name);
        assertNotNull("filename", filename);
        assertNotNull("suites", suites);

        this.name = name;
        this.filename = filename;
        this.suites = suites;
    }

    private void assertNotNull(String paramName, Object param){
        if(null == param){
            throw new IllegalArgumentException("TestProject constructor parameter '" + paramName + "' must be non-null!");
        }
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