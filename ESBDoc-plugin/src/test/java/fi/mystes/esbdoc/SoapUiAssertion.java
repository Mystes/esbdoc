package fi.mystes.esbdoc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by mystes-am on 31.5.2016.
 */
public class SoapUiAssertion {
    private JsonObject soapUiProject;

    private SoapUiAssertion(){};

    public SoapUiAssertion(Set<Map.Entry<String, JsonElement>> tests, String name){

        boolean soapUiProjectFound = false;
        for(Map.Entry<String, JsonElement> test : tests){
            JsonArray soapUiProjectCandidateArray = test.getValue().getAsJsonArray();
            Iterator<JsonElement> iterator = soapUiProjectCandidateArray.iterator();
            while(!soapUiProjectFound && iterator.hasNext()) {
                JsonObject soapUiProjectCandidate = iterator.next().getAsJsonObject();
                String candidateName = soapUiProjectCandidate.get("project").getAsString();
                if (StringUtils.equals(name, candidateName)) {
                    this.soapUiProject = soapUiProjectCandidate;
                    soapUiProjectFound = true;
                }
            }
            if(soapUiProjectFound){
                return;
            }
        }
        assertTrue("Cannot do SoapUI Project assertions: Could not locate SoapUI project with name: " + name, soapUiProjectFound);
    }

    public void assertFilename(String expected){
        String actual = this.soapUiProject.get("filename").getAsString();
        assertThat("SoapUI filename mismatch.", actual, is(expected));
    }
}
