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
 * TODO This is horrible
 */
public class JsonWriter {
    private static Log log = LogFactory.getLog(JsonWriter.class);
    
    public static final String RESOURCES = "resources";
    public static final String TYPE = "type";
    public static final String DEPENDENCIES = "dependencies";
    public static final String FORWARD = "forward";
    public static final String TARGET = "target";
    public static final String REVERSE = "reverse";
    public static final String SOURCE = "source";
    public static final String TESTS = "tests";
    public static final String PROJECT = "project";
    public static final String FILENAME = "filename";
    public static final String SUITES = "suites";
    public static final String NAME = "name";
    public static final String CASES = "cases";
    public static final String PURPOSE = "purpose";
    public static final String RECEIVES = "receives";
    public static final String RETURNS = "returns";
    public static final String DESCRIPTION = "description";
    public static final String FIELDS = "fields";
    public static final String PATH = "path";
    public static final String OPTIONAL = "optional";
    public static final String EXAMPLE = "example";

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

            for (Dependency d : entry.getValue()) {
                // Currently only Artifacts are included in the JSON output
                if (d.getDependency() instanceof Artifact) {
                    generator.writeStartObject();
                    generator.writeStringField(TARGET, ((Artifact) d.getDependency()).getName());
                    generator.writeStringField(TYPE, d.getType().toString());
                    generator.writeEndObject();
                }
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();

        generator.writeObjectFieldStart(REVERSE);
        for (Entry<Artifact, Set<Dependency>> entry : reverseDependencyMap.entrySet()) {
            generator.writeArrayFieldStart(entry.getKey().getName());
            for (Dependency d : entry.getValue()) {
                generator.writeStartObject();
                generator.writeStringField(SOURCE, d.dependent.getName());
                generator.writeStringField(TYPE, d.getType().toString());
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();
        generator.writeEndObject();

        generator.writeObjectFieldStart(TESTS);
        for (Entry<String, Set<TestProject>> entry : testsMap.entrySet()) {
            generator.writeArrayFieldStart(entry.getKey());
            for (TestProject p : entry.getValue()) {
                generator.writeStartObject();
                generator.writeStringField(PROJECT, p.getName());
                generator.writeStringField(FILENAME, p.getFilename());
                generator.writeArrayFieldStart(SUITES);
                for (TestSuite s : p.getTestSuites()) {
                    generator.writeStartObject();
                    generator.writeStringField(NAME, s.getName());
                    generator.writeArrayFieldStart(CASES);
                    for (TestCase c : s.getTestCases()) {
                        generator.writeStartObject();
                        generator.writeStringField(NAME, c.getName());
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
