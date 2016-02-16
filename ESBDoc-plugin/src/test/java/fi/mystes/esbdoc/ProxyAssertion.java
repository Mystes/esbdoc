package fi.mystes.esbdoc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by mystes-am on 16.2.2016.
 */
public class ProxyAssertion {
    private JsonObject proxy;

    private ProxyAssertion(){};

    public ProxyAssertion(Set<Map.Entry<String, JsonElement>> resources, String name){

        boolean proxyFound = false;
        for(Map.Entry<String, JsonElement> resource : resources){
            if(StringUtils.equals(name, resource.getKey())){
                JsonObject proxyCandidate = resource.getValue().getAsJsonObject();
                assertThat(proxyCandidate.get("type").getAsString(), is("proxy"));
                proxyFound = true;
                this.proxy = proxyCandidate;
            }
        }
        assertTrue(proxyFound);
    }

    public void assertPurpose(String expected){
        String actual = this.proxy.get("purpose").getAsString();
        assertThat(actual, is(expected));
    }
}
