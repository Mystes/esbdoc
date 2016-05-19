package fi.mystes.esbdoc;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by mystes-am on 5.4.2016.
 */
public class EndpointAssertion {
    private JsonObject endpoint;

    private EndpointAssertion(){};

    public EndpointAssertion(Set<Map.Entry<String, JsonElement>> resources, String name){

        boolean endpointFound = false;
        for(Map.Entry<String, JsonElement> resource : resources){
            if(StringUtils.equals(name, resource.getKey())){
                JsonObject endpointCandidate = resource.getValue().getAsJsonObject();
                String actualType = endpointCandidate.get("type").getAsString();
                String faultyAssertionExplanation = "Found an endpoint candidate with name: " + name + " but its type was: " + actualType;
                assertThat(faultyAssertionExplanation, actualType, is("endpoint"));
                endpointFound = true;
                this.endpoint = endpointCandidate;
            }
        }
        assertTrue("Cannot do endpoint assertions: Could not locate endpoint with name: " + name, endpointFound);
    }

    public void assertPurpose(String expected){
        String actual = this.endpoint.get("purpose").getAsString();
        assertThat("Endpoint description mismatch.", actual, is(expected));
    }
}
