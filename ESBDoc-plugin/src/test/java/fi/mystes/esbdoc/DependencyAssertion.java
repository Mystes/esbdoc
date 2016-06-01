package fi.mystes.esbdoc;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by mystes-am on 16.2.2016.
 */
class DependencyAssertion {

    private Direction direction;
    private Collection<Map.Entry<String, JsonElement>> dependencies;
    private Collection<Map.Entry<String, JsonElement>> tests;

    private String artifactName;
    private EndpointModel endpointModel;

    public enum EndpointModel {
        EXCLUSIVELY, NON_EXCLUSIVELY, NOWHERE
    }

    private DependencyAssertion() {
    }

    public DependencyAssertion(String artifactName, Set<Map.Entry<String, JsonElement>> allDependencies, Set<Map.Entry<String, JsonElement>> allTests){
        this.dependencies = allDependencies;
        this.artifactName = artifactName;
        this.tests = allTests;
    }

    public void testedBy(String... expectedTestNames){
        if(null == expectedTestNames){
            return;
        }

        String[] actualTestNames = getTestNamesForThisArtifact();

        assertEquals("Number of expected and actual tests does not match for artifact: " + artifactName , expectedTestNames.length, actualTestNames.length);
        assertTestNames(expectedTestNames, actualTestNames);
    }

    private String[] getTestNamesForThisArtifact(){
        JsonElement element = testMap().get(artifactName);
        JsonArray testArray = element.getAsJsonArray();
        List<String> actualTestNames = getTestNamesFromArray(testArray);
        return actualTestNames.toArray(new String[actualTestNames.size()]);
    }

    private List<String> getTestNamesFromArray(JsonArray testArray){
        List<String> actualTestNames = new ArrayList<String>();
        Iterator<JsonElement> iterator = testArray.iterator();
        while (iterator.hasNext()){
            JsonObject test = iterator.next().getAsJsonObject();
            String testName = test.get("project").getAsString();
            actualTestNames.add(testName);
        }
        return actualTestNames;
    }

    private Map<String, JsonElement> testMap(){
        Map<String, JsonElement> testMap = new HashMap<String, JsonElement>();
        for(Map.Entry entry : tests){
            testMap.put((String) entry.getKey(), (JsonElement) entry.getValue());
        }
        return testMap;
    }

    private void assertTestNames(String[] expectedTestNames, String[] actualTestNames){
        for(String expectedName : expectedTestNames){
            boolean matchFound = false;
            for(String actualName : actualTestNames){
                if(StringUtils.equals(expectedName, actualName)){
                    matchFound = true;
                }
                assertTrue("Expected artifact '" + artifactName +  "' to be tested by '" + expectedName + "' but it is not.", matchFound);
            }
        }
    }

    public TypeAssertion forwardsTo(EndpointModel endpointModel, String... endpointNames) {
        this.direction = Direction.FORWARD;
        return pointsTo(endpointModel, endpointNames);
    }

    public TypeAssertion reversesTo(EndpointModel endpointModel, String... endpointNames) {
        this.direction = Direction.REVERSE;
        return pointsTo(endpointModel, endpointNames);
    }

    private TypeAssertion pointsTo(EndpointModel endpointModel, String... endpointNames) {
        this.endpointModel = endpointModel;
        Collection<Map.Entry<String, JsonElement>> allDirectedDependencies = getDirectedDependencies(this.direction, this.dependencies);
        Optional<Map.Entry<String, JsonElement>> directedDependenciesForArtifact = allDirectedDependencies.stream().filter(new ArtifactFilter()).findAny();

        if(endpointModel.equals(EndpointModel.NOWHERE)){
            assertDependenciesToNowhere(directedDependenciesForArtifact);
            return new TypeAssertion((String[]) null);
        }

        List<String> dependencyNames = getDependencyNames(directedDependenciesForArtifact);

        if(endpointModel.equals(EndpointModel.NON_EXCLUSIVELY)){
            assertNonExclusiveDependencies(dependencyNames, endpointNames);
        }

        if(endpointModel.equals(EndpointModel.EXCLUSIVELY)){
            assertExclusiveDependencies(dependencyNames, endpointNames);
        }

        return new TypeAssertion(endpointNames);
    }

    class TypeAssertion {
        private String[] endpointNames;
        private List<Dependency> endpoints = new ArrayList<Dependency>();

        private TypeAssertion(String... endpointNames){
            this.endpointNames = endpointNames;

            if(null == endpointNames){
                return;
            }

            Collection<Map.Entry<String, JsonElement>> allDirectedDependencies = getDirectedDependencies(direction, dependencies);
            Optional<Map.Entry<String, JsonElement>> directedDependenciesForArtifact = allDirectedDependencies.stream().filter(new ArtifactFilter()).findAny();
            Iterator<JsonElement> directedDependencies = directedDependenciesForArtifact.get().getValue().getAsJsonArray().iterator();
            while(directedDependencies.hasNext()){
                JsonObject current = directedDependencies.next().getAsJsonObject();
                this.endpoints.add(new Dependency(current));
            }
        }

        public void asType(DependencyType expectedType){
            if(null == this.endpointNames){
                assertTrue("You cannot set type expectations when there are no dependencies!", false);
            }

            for(String endpointName : this.endpointNames){
                assertEquals("Dependency from '" + artifactName + "' to '" + endpointName + "' type did not match expectation.", expectedType.toString(), dependency(endpointName).getType());
            }
        }

        private Dependency dependency(String name){
            for(Dependency dependency : this.endpoints){
                if(StringUtils.equals(dependency.getName(), name)){
                    return dependency;
                }
            }
            throw new EsbDocException("Could not get dependency by name. This should have been impossible. Dependency name: " + name);
        }

        private class Dependency {
            private String name;
            private String type;

            private Dependency(JsonObject jsonDependency){
                JsonElement name = jsonDependency.has("target") ? jsonDependency.get("target") : jsonDependency.get("source");
                JsonElement type = jsonDependency.get("type");
                this.name = name.getAsString();
                this.type = type.getAsString();
            }

            private String getName(){
                return this.name;
            }

            private String getType(){
                return this.type;
            }
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
        if(!directedDependenciesForArtifact.isPresent()){
            return targets;
        }
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
