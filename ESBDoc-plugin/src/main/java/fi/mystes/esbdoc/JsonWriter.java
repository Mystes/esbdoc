/**
 * Copyright 2018 Mystes Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.mystes.esbdoc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Created by mystes-am on 29.5.2015.
 * TODO The construct of of methods in this class might need refactoring
 */
public class JsonWriter {
    private static Log log = LogFactory.getLog(JsonWriter.class);

    private static final String RESOURCES = "resources";
    private static final String TYPE = "type";
    private static final String DEPENDENCIES = "dependencies";
    private static final String FORWARD = "forward";
    private static final String TARGET = "target";
    private static final String REVERSE = "reverse";
    private static final String SOURCE = "source";
    private static final String TESTS = "tests";
    private static final String PROJECT = "project";
    private static final String FILENAME = "filename";
    private static final String SUITES = "suites";
    private static final String NAME = "name";
    private static final String CASES = "cases";
    private static final String PURPOSE = "purpose";
    private static final String RECEIVES = "receives";
    private static final String RETURNS = "returns";
    private static final String DESCRIPTION = "description";
    private static final String FIELDS = "fields";
    private static final String PATH = "path";
    private static final String OPTIONAL = "optional";
    private static final String EXAMPLE = "example";

    private ArtifactMap artifactMap;
    private TestMap testsMap;
    private ArtifactDependencyMap forwardDependencyMap;
    private ArtifactDependencyMap reverseDependencyMap;


    public JsonWriter(ArtifactDependencyMap forwardDependencyMap, ArtifactDependencyMap reverseDependencyMap, TestMap testsMap, ArtifactMap artifactMap){
        this.artifactMap = artifactMap;
        this.testsMap = testsMap;
        this.forwardDependencyMap = forwardDependencyMap;
        this.reverseDependencyMap = reverseDependencyMap;
    }

    private JsonGenerator createGeneratorFor(OutputStream outputStream) throws IOException{
        JsonFactory factory = new JsonFactory();
        return factory.createGenerator(outputStream);
    }

    public void writeJson(OutputStream outputStream) throws IOException {
        JsonGenerator generator = createGeneratorFor(outputStream);
        generator.writeStartObject();

        generator.writeObjectFieldStart(RESOURCES);
        for (Artifact artifact : artifactMap.getArtifacts()) {
            generator.writeObjectFieldStart(artifact.getName());
            if (artifact.isDescriptionDefined()) {
                writeArtifactDescriptionJson(artifact.description, generator);
            }
            generator.writeStringField(TYPE, artifact.getType().toString());
            generator.writeEndObject();
        }
        generator.writeEndObject();

        generator.writeObjectFieldStart(DEPENDENCIES);

        generator.writeObjectFieldStart(FORWARD);
        for (Entry<Artifact, Set<Dependency>> entry : forwardDependencyMap.entrySet()) {
            generator.writeArrayFieldStart(entry.getKey().getName());

            for (Dependency dependency : entry.getValue()) {
                // Currently only Artifacts are included in the JSON output
            	boolean ainoDependency = dependency.getType() == DependencyType.AINO_LOG;
            	boolean registryDependency = dependency.getType() == DependencyType.REGISTRY;
            	boolean isArtifact = dependency.getDependency() instanceof Artifact;
                if (isArtifact || ainoDependency || registryDependency) {
                    generator.writeStartObject();
                    generator.writeStringField(TARGET, isArtifact?((Artifact) dependency.getDependency()).getName():dependency.getDependency().toString());
                    generator.writeStringField(TYPE, dependency.getType().toString());
                    generator.writeEndObject();
                }
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();

        generator.writeObjectFieldStart(REVERSE);
        for (Entry<Artifact, Set<Dependency>> entry : reverseDependencyMap.entrySet()) {
            generator.writeArrayFieldStart(entry.getKey().getName());
            for (Dependency dependency : entry.getValue()) {
                generator.writeStartObject();
                generator.writeStringField(SOURCE, dependency.dependent.getName());
                generator.writeStringField(TYPE, dependency.getType().toString());
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();
        generator.writeEndObject();

        generator.writeObjectFieldStart(TESTS);
        for (Entry<String, Set<TestProject>> entry : testsMap.entrySet()) {
            generator.writeArrayFieldStart(entry.getKey());
            for (TestProject testProject : entry.getValue()) {
                generator.writeStartObject();
                generator.writeStringField(PROJECT, testProject.getName());
                generator.writeStringField(FILENAME, testProject.getFilename());
                generator.writeArrayFieldStart(SUITES);
                for (TestSuite testSuite : testProject.getTestSuites()) {
                    generator.writeStartObject();
                    generator.writeStringField(NAME, testSuite.getName());
                    generator.writeArrayFieldStart(CASES);
                    for (TestCase testCase : testSuite.getTestCases()) {
                        generator.writeStartObject();
                        generator.writeStringField(NAME, testCase.getName());
                        generator.writeEndObject();
                    }
                    generator.writeEndArray();
                    generator.writeEndObject();
                }
                generator.writeEndArray();
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();

        generator.writeEndObject();
        generator.close();
    }

    private void writeArtifactDescriptionJson(ArtifactDescription artifactDescription, JsonGenerator generator) throws IOException {
        if (artifactDescription.isPurposeDefined()) {
            generator.writeStringField(PURPOSE, artifactDescription.purpose);
        }

        if (artifactDescription.isReceivesDefined()) {
            generator.writeObjectFieldStart(RECEIVES);
            writeArtifactInterfaceInfoJson(artifactDescription.receives, generator);
            generator.writeEndObject();
        }

        if (artifactDescription.isReturnsDefined()) {
            generator.writeObjectFieldStart(RETURNS);
            writeArtifactInterfaceInfoJson(artifactDescription.returns, generator);
            generator.writeEndObject();
        }
    }

    private void writeArtifactInterfaceInfoJson(ArtifactInterfaceInfo artifactInterfaceInfo, JsonGenerator generator) throws IOException {
        if (artifactInterfaceInfo.isDescriptionDefined()) {
            generator.writeStringField(DESCRIPTION, removeLineBreaks(artifactInterfaceInfo.description));
        }

        if (artifactInterfaceInfo.isFieldsDefined()) {
            generator.writeArrayFieldStart(FIELDS);
            for (ArtifactIntefaceField artifactIntefaceField : artifactInterfaceInfo.fields) {
                generator.writeStartObject();
                if (artifactIntefaceField.isDescriptionDefined()) {
                    generator.writeStringField(DESCRIPTION, StringEscapeUtils.escapeJson(removeLineBreaks(artifactIntefaceField.description)));
                } else {
                    generator.writeStringField(DESCRIPTION, "");
                    log.warn(artifactIntefaceField.getArtifactName() + ": Has empty description field.");
                }
                generator.writeStringField(PATH, artifactIntefaceField.path);
                generator.writeBooleanField(OPTIONAL, artifactIntefaceField.optional);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }

        if (artifactInterfaceInfo.isExampleDefined()) {
            generator.writeStringField(EXAMPLE, removeLineBreaks(artifactInterfaceInfo.example));
        }
    }

    private String removeLineBreaks(String text) {
        if(StringUtils.isBlank(text)){
            return null;
        }
        return StringEscapeUtils.escapeJson(text.replace("\n", "").replace("\r", "").replace("\r\n", ""));
    }
}
