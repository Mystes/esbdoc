package fi.mystes.esbdoc;

import com.google.gson.JsonElement;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * Created by mystes-am on 16.2.2016.
 */
class DependencyTypeAssertion {

    private Direction direction;
    private Set<Map.Entry<String, JsonElement>> dependencies;

    private DependencyTypeAssertion() {
    }

    public DependencyTypeAssertion(Set<Map.Entry<String, JsonElement>> allDependencies) {
        this.direction = Direction.BOTH;
        this.dependencies = getDirectedDependencies(Direction.FORWARD, allDependencies);
        this.dependencies.addAll(getDirectedDependencies(Direction.REVERSE, allDependencies));
    }

    public DependencyTypeAssertion(Direction direction, Set<Map.Entry<String, JsonElement>> allDependencies) {
        this.direction = direction;
        this.dependencies = getDirectedDependencies(direction, allDependencies);
    }

    private Set<Map.Entry<String, JsonElement>> getDirectedDependencies(Direction direction, Set<Map.Entry<String, JsonElement>> allDependencies) {
        for (Map.Entry<String, JsonElement> directedDependencies : allDependencies) {
            if (StringUtils.equals(direction.toString(), directedDependencies.getKey())) {
                return directedDependencies.getValue().getAsJsonObject().entrySet();
            }
        }
        throw new RuntimeException("Could not get directed dependencies. This should have been impossible.");
    }

    public void assertEmpty() {
        assertTrue(this.dependencies.isEmpty());
    }
}
