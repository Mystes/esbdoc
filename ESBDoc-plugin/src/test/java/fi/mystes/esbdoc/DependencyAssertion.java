package fi.mystes.esbdoc;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by mystes-am on 16.2.2016.
 */
class DependencyAssertion {

    private Direction direction;
    private Collection<Map.Entry<String, JsonElement>> dependencies;

    private String artifactName;
    private EndpointModel endpointModel;

    public enum EndpointModel {
        EXCLUSIVELY, NON_EXCLUSIVELY, NOWHERE
    }

    private DependencyAssertion() {
    }

    public DependencyAssertion(String artifactName, Set<Map.Entry<String, JsonElement>> allDependencies){
        this.dependencies = allDependencies;
        this.artifactName = artifactName;
    }

    public void forwardsTo(EndpointModel endpointModel, String... endpointNames) {
        this.direction = Direction.FORWARD;
        pointsTo(endpointModel, endpointNames);
    }

    public void reversesTo(EndpointModel endpointModel, String... endpointNames) {
        this.direction = Direction.REVERSE;
        pointsTo(endpointModel, endpointNames);
    }

    private void pointsTo(EndpointModel endpointModel, String... endpointNames) {
        this.endpointModel = endpointModel;
        Collection<Map.Entry<String, JsonElement>> allDirectedDependencies = getDirectedDependencies(this.direction, this.dependencies);
        Optional<Map.Entry<String, JsonElement>> directedDependenciesForArtifact = allDirectedDependencies.stream().filter(new ArtifactFilter()).findAny();

        if(endpointModel.equals(EndpointModel.NOWHERE)){
            assertDependenciesToNowhere(directedDependenciesForArtifact);
            return;
        }

        List<String> dependencyNames = getDependencyNames(directedDependenciesForArtifact);

        if(endpointModel.equals(EndpointModel.NON_EXCLUSIVELY)){
            assertNonExclusiveDependencies(dependencyNames, endpointNames);
        }

        if(endpointModel.equals(EndpointModel.EXCLUSIVELY)){
            assertExclusiveDependencies(dependencyNames, endpointNames);
        }
    }

    private void assertDependenciesToNowhere(Optional<Map.Entry<String, JsonElement>> directedDependenciesForArtifact) {
        String faultString = "Expected artifact '" + this.artifactName + "' to have no " + this.direction + " dependencies but it has.";
        assertFalse(faultString, directedDependenciesForArtifact.isPresent());
    }

    private void assertNonExclusiveDependencies(List<String> targets, String[] endpointNames) {
        String faultString = "Expected artifact '" + this.artifactName + "' to ";
        faultString += this.direction + " to at least all of these places: " + ArrayUtils.toString(endpointNames) + " but it ";
        faultString += this.direction  + "s to these ones: " + targets;

        boolean allReferencesFound = CollectionUtils.containsAll(targets, Arrays.asList(endpointNames));
        assertTrue(faultString, allReferencesFound);
    }

    private void assertExclusiveDependencies(List<String> targets, String[] endpointNames) {
        String faultString = "Expected artifact '" + this.artifactName + "' to ";
        faultString += this.direction  + " to ONLY these places: " + ArrayUtils.toString(endpointNames) + " but it ";
        faultString += this.direction + "s to these ones: " + targets;

        boolean allReferencesFound = CollectionUtils.containsAll(targets, Arrays.asList(endpointNames));
        boolean noOtherReferences = CollectionUtils.disjunction(targets, Arrays.asList(endpointNames)).isEmpty();
        boolean exclusionSatisfied = allReferencesFound && noOtherReferences;
        assertTrue(faultString, exclusionSatisfied);
    }

    private List<String> getDependencyNames(Optional<Map.Entry<String, JsonElement>> directedDependenciesForArtifact) {
        List<String> targets = new ArrayList<String>();
        Iterator<JsonElement> endpoints = directedDependenciesForArtifact.get().getValue().getAsJsonArray().iterator();
        while(endpoints.hasNext()){
            JsonObject currentEndpoint = endpoints.next().getAsJsonObject();
            JsonElement target = currentEndpoint.has("target") ? currentEndpoint.get("target") : currentEndpoint.get("source");
            targets.add(target.getAsString());
        }
        return targets;
    }

    private class ArtifactFilter implements java.util.function.Predicate<Map.Entry<String, JsonElement>> {

        @Override
        public boolean test(Map.Entry<String, JsonElement> currentEntry) {
            return StringUtils.equals(currentEntry.getKey(), artifactName);
        }
    }

    public DependencyAssertion(Collection<Map.Entry<String, JsonElement>> allDependencies) {
        this.direction = Direction.BOTH;

        Collection<Map.Entry<String, JsonElement>> forward = getDirectedDependencies(Direction.FORWARD, allDependencies);
        Collection<Map.Entry<String, JsonElement>> reverse = getDirectedDependencies(Direction.REVERSE, allDependencies);
        this.dependencies = CollectionUtils.union(forward, reverse);
    }

    public DependencyAssertion(Direction direction, Set<Map.Entry<String, JsonElement>> allDependencies) {
        this.direction = direction;
        this.dependencies = getDirectedDependencies(direction, allDependencies);
    }

    private Collection<Map.Entry<String, JsonElement>> getDirectedDependencies(Direction direction, Collection<Map.Entry<String, JsonElement>> allDependencies) {
        for (Map.Entry<String, JsonElement> directedDependencies : allDependencies) {
            if (StringUtils.equals(direction.toString(), directedDependencies.getKey())) {
                return directedDependencies.getValue().getAsJsonObject().entrySet();
            }
        }
        throw new EsbDocException("Could not get directed dependencies. This should have been impossible. Direction: " + direction);
    }

    public void assertEmpty() {
        assertTrue("Expected dependency direction '" + direction + "' to be empty but it was not.", this.dependencies.isEmpty());
    }
}
