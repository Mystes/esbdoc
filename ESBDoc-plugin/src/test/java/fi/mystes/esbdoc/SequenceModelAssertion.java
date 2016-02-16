package fi.mystes.esbdoc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by mystes-am on 16.2.2016.
 */
public class SequenceModelAssertion{
    private String jsonString;
    private JsonObject json;
    private JsonArray sequenceModels;

    private SequenceModelAssertion(){};

    public SequenceModelAssertion(String esbDocSequencePath) throws IOException {
        File sequenceFile = new File(esbDocSequencePath);
        assertTrue("File does not exist: " + esbDocSequencePath, sequenceFile.exists());

        this.jsonString = FileUtils.readFileToString(sequenceFile);
        this.json = new Gson().fromJson(this.jsonString, JsonObject.class);
        assertTrue("File does not contain expected element: models", json.has("models"));

        JsonObject models = json.getAsJsonObject("models");
        assertTrue("models-element does not contain expected element: sequence-models", models.has("sequence-models"));

        this.sequenceModels = models.getAsJsonArray("sequence-models");
        assertNotNull("sequence-models element is null and that's not OK.", this.sequenceModels);
    }

    public void assertSize(int expected){
        assertThat(this.sequenceModels.size(), is(expected));
    }

    public void assertContains(String expected){
        boolean matchFound = false;
        Iterator<JsonElement> iterator = this.sequenceModels.iterator();
        while(iterator.hasNext()){
            JsonObject currentElement = iterator.next().getAsJsonObject();
            String actual = currentElement.get("name").getAsString();
            if(StringUtils.equals(expected, actual)){
                matchFound = true;
            }
        }

        assertTrue(matchFound);
    }

}
