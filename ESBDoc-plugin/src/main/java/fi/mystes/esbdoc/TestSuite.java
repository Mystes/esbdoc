package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

import java.util.Set;

/**
 * Represents a single TestSuite
 */
public class TestSuite implements Comparable<TestSuite> {

    private final String name;
    private final Set<TestCase> cases;

    public TestSuite(String name, Set<TestCase> cases) {
        if (name == null || cases == null) {
            throw new IllegalArgumentException("All TestSuite constructor parameters must be non-null");
        }

        this.name = name;
        this.cases = cases;
    }

    public String getName() {
        return name;
    }

    public Set<TestCase> getTestCases() {
        return cases;
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
    public int compareTo(TestSuite suite) {
        int difference = this.name.compareTo(suite.name);
        return difference;
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof TestSuite) {
            TestSuite other = (TestSuite) o;

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
