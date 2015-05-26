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
        if (name == null) {
            throw new IllegalArgumentException("All TestCase constructor parameters must be non-null");
        }

        this.name = name;
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
