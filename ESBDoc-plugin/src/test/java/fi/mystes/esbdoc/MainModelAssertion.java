package fi.mystes.esbdoc;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by mystes-am on 16.2.2016.
 */
public class MainModelAssertion {
    private String jsonString;
    private JsonObject json;
    private Set<Map.Entry<String, JsonElement>> resources;
    private Set<Map.Entry<String, JsonElement>> dependencies;
    private Set<Map.Entry<String, JsonElement>> tests;

    private MainModelAssertion(){};

    public MainModelAssertion(String esbDocProxyPath) throws IOException {
        File proxyFile = new File(esbDocProxyPath);
        assertTrue("File does not exist: " + esbDocProxyPath, proxyFile.exists());

        this.jsonString = FileUtils.readFileToString(proxyFile);
        this.json = new Gson().fromJson(this.jsonString, JsonObject.class);

        assertTrue("File does not contain expected element: resources", json.has("resources"));
        assertTrue("File does not contain expected element: dependencies", json.has("dependencies"));
        assertTrue("File does not contain expected element: tests", json.has("tests"));

        this.resources = json.get("resources").getAsJsonObject().entrySet();
        this.dependencies = json.get("dependencies").getAsJsonObject().entrySet();
        this.tests = json.get("tests").getAsJsonObject().entrySet();

        assertNotNull("resource set is null and that's not OK.", this.resources);
        assertNotNull("dependency set is null and that's not OK.", this.dependencies);
        assertNotNull("test set is null and that's not OK.", this.tests);
    }

    public void assertNoTests(){
        assertThat(this.tests.size(), is(0));
    }

    public void assertHasTests(){
        assertThat(this.tests.size(), not(is(0)));
    }

    public void assertNoForwardDependencies() {
        new DependencyAssertion(Direction.FORWARD, this.dependencies).assertEmpty();
    }

    public void assertNoReverseDependencies() {
        new DependencyAssertion(Direction.REVERSE, this.dependencies).assertEmpty();
    }

    public void assertNoDependencies() {
        assertNoForwardDependencies();
        assertNoReverseDependencies();
    }

    public ProxyAssertion proxyAssertionFor(String proxyName) {
        return new ProxyAssertion(this.resources, proxyName);
    }

    public SequenceAssertion sequenceAssertionFor(String sequenceName) { return new SequenceAssertion(this.resources, sequenceName); }

    public EndpointAssertion endpointAssertionFor(String endpointName) { return new EndpointAssertion(this.resources, endpointName); }

    public DependencyAssertion dependencyAssertionFor(String artifactName) {
        return new DependencyAssertion(artifactName, this.dependencies, this.tests);
    }

    public SoapUiAssertion soapUiAssertionFor(String soapUiProjectName) {
        return new SoapUiAssertion(this.tests, soapUiProjectName);
    }
}
