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
            throw new IllegalArgumentException("All TestSuite constructor parameters must be non-null");
        }

        this.name = name;
    }

    public String getName() {
        return name;
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
    public int compareTo(TestCase c) {
        int difference = this.name.compareTo(c.name);
        return difference;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof TestCase) {
            TestCase other = (TestCase) o;

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
