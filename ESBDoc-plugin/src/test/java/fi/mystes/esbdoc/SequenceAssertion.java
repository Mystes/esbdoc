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
public class SequenceAssertion {
    private JsonObject sequence;

    private SequenceAssertion(){};

    public SequenceAssertion(Set<Map.Entry<String, JsonElement>> resources, String name){

        boolean sequenceFound = false;
        for(Map.Entry<String, JsonElement> resource : resources){
            if(StringUtils.equals(name, resource.getKey())){
                JsonObject sequenceCandidate = resource.getValue().getAsJsonObject();
                String actualType = sequenceCandidate.get("type").getAsString();
                String faultyAssertionExplanation = "Found a sequence candidate with name: " + name + " but its type was: " + actualType;
                assertThat(faultyAssertionExplanation, actualType, is("sequence"));
                sequenceFound = true;
                this.sequence = sequenceCandidate;
            }
        }
        assertTrue("Cannot do sequence assertions: Could not locate sequence with name: " + name, sequenceFound);
    }

    public void assertPurpose(String expected){
        String actual = this.sequence.get("purpose").getAsString();
        assertThat("Sequence description mismatch.", actual, is(expected));
    }
}
