package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 26.5.2015.
 */
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
