package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

/**
 * Represents a single TestSuite
 */
public class TestCase implements Comparable<TestCase> {

    private final String name;

    public TestCase(String name) {
        assertNotNull("name", name);

        this.name = name;
    }

    private void assertNotNull(String paramName, Object param){
        if(null == param){
            throw new IllegalArgumentException("TestCase constructor parameter '" + paramName + "' must be non-null!");
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "{name: \"" + name + "\"}";
    }

    @Override
    public int compareTo(TestCase that) {
        return this.name.compareTo(that.name);
    }

    @Override
    public boolean equals(Object object) {
        if(null == object){
            return false;
        }
        if(!(object instanceof TestCase)){
            return false;
        }
        TestCase that = (TestCase) object;
        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return 37 * 37 + name.hashCode(); //TODO why like this?
    }
}
