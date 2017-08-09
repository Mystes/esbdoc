package fi.mystes.esbdoc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.util.*;

public class SequenceDiagramBuilder {
    private static Log log = LogFactory.getLog(SequenceDiagramBuilder.class);

    private static final String PATH_TO_ESB = "/home/kreshnikg/Applications/wso2esb-4.5.1"; //FIXME that's user-specific
    private static final String RELATIVE_PATH_TO_CONFIGURATION_FILES = "/repository/deployment/server/synapse-configs/default/";

    private static SequenceDiagramBuilder instance = null;
    public String filesHome;
    private HashMap<String, SequenceItem> sequenceItemMap = new HashMap();
    public Map<String, String> visited = new HashMap<String, String>();
    public StringBuilder output = null;

    private SequenceDiagramBuilder(String wso2Home) {
        filesHome = wso2Home + RELATIVE_PATH_TO_CONFIGURATION_FILES;
    }

    private SequenceDiagramBuilder() { }

    public static SequenceDiagramBuilder instance() {
        if (null == instance) {
            log.info("Creating a new instance of the SequenceDiagramBuilder.");
            instance = new SequenceDiagramBuilder();
        }
        return instance;
    }

    public static void purge() {
        instance = new SequenceDiagramBuilder();
    }

    public static void main(String[] args) {
        //TODO What information does magic argument number seven carry and what about the other args?
        String result = new SequenceDiagramBuilder(PATH_TO_ESB).buildPipe(args[7]);
        log.info(result);
    }

    private SAXParser parser() throws SAXException, ParserConfigurationException{
        log.debug("Instantiating a new SAX Parser...");
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
        return parserFactory.newSAXParser();
    }

    public String buildPipe(String file) {
        try {
            log.info("Building a pipe from file.");
            //TODO What pipe? A single pipe or many pipes?
            //TODO why should the processing be different when building from inputstream than when building from string?
            visited = new HashMap<String, String>();
            output = new StringBuilder();
            build(file);
            return output.toString();
        } catch (SAXException e) {
            log.error("buildPipe encountered a SAX Exception: " + e.getMessage());
        } catch (IOException e) {
            log.error("buildPipe encountered an IO Exception: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            log.error("buildPipe encountered a Parser Configuration Exception: " + e.getMessage());
        }
        return null;
    }

    public String buildPipe(InputStream is) {
        try {
            visited = new HashMap<String, String>();
            output = new StringBuilder();
            build(is);
            //TODO wtf does this condition mean?
            //TODO why should the processing be different when building from inputstream than when building from string?
            if (output.length() > 6) {
                SequenceItem item = createSequenceItem(output.toString());
                saveItemForFurtherProcessing(item);
            }
            return output.toString();
        } catch (SAXException e) {
            log.error("buildPipe encountered a SAX Exception: " + e.getMessage());
        } catch (IOException e) {
            log.error("buildPipe encountered an IO Exception: " + e.getMessage());
        } catch (ParserConfigurationException e) {
            log.error("buildPipe encountered a Parser Configuration Exception: " + e.getMessage());
        }
        return null;
    }

    public SequenceDiagramBuilder build(String file) throws SAXException, IOException, ParserConfigurationException {
        log.info("Trying to parse from file: " + file);
        parser().parse(new FileInputStream(filesHome + file), getNewHandler());
        return this;
    }

    private SequenceDiagramBuilder build(InputStream is) throws SAXException, IOException, ParserConfigurationException {
        log.info("Trying to parse from InputStream...: ");
        parser().parse(is, getNewHandler());
        return this;
    }

    public SAXHandler getNewHandler() {
        return new SAXHandler(this);
    }

    private void saveItemForFurtherProcessing(SequenceItem item) {
        getSequenceItemMap().put(item.getName(), item);
    }

    public HashMap<String, SequenceItem> getSequenceItemMap() {
        return sequenceItemMap;
    }

    private SequenceItem createSequenceItem(String source) {
        if(StringUtils.isEmpty(source)){
            return null;
        }
        return new SequenceItem(source);
    }

    public void writeOutputFile(String outputFilename) throws IOException {
        log.info("Writing out: " + outputFilename);

        JsonGenerator generator = createJsonGenerator(outputFilename);

        generator.writeStartObject();
        generator.writeObjectFieldStart("models");
        generator.writeArrayFieldStart("sequence-models");

        writeSequenceItems(generator);

        generator.writeEndArray();
        generator.writeEndObject();
        generator.close();
    }

    private void writeSequenceItems(JsonGenerator generator) throws IOException {
        Set<String> keys = getSequenceItemMap().keySet();
        for (String key : keys) {
            log.info("Jasonifying: " + key);
            generator.writeStartObject();
            generator.writeStringField("name", key);
            generator.writeStringField("description", "");

            String sequenceItemString = generateSequenceItemString(key);
            generator.writeStringField("sequence", StringEscapeUtils.escapeJson(sequenceItemString));

            generator.writeEndObject();
        }
    }

    private String generateSequenceItemString(String key) throws IOException {
        return printDependenciesRecursively(new SequenceItemParameters(key));
    }

    private JsonGenerator createJsonGenerator(String outputFilename) throws IOException {
        FileOutputStream fis = new FileOutputStream(new File(outputFilename));
        JsonFactory factory = new JsonFactory();
        return factory.createGenerator(fis);
    }

    private String printDependenciesRecursively(SequenceItemParameters params) throws IOException {
        String key = params.getKey();
        String parent = params.getParent();
        boolean parentExists = params.hasParent();

        if (params.containsCircularDependencies()) {
            return params.toString();
        }

        if (parentExists) {
            params.addDependency(parent, key);
        }

        params.addHandledNode(key);
        populateLeaves(params);

        if (parentExists) {
            params.addDependency(key, parent);
        }

        return params.toString();
    }

    private void populateLeaves(SequenceItemParameters params) throws IOException {
        String key = params.getKey();
        SequenceItem item = getSequenceItemMap().get(key);
        List<String> leaves =  getLeaves(item);

        for (String leaf : leaves) {
            params.setParent(key);
            params.setKey(leaf);
            printDependenciesRecursively(params);
        }
    }

    private List<String> getLeaves(SequenceItem item){
        if (null == item) {
            return new ArrayList<String>();
        }
        return item.getLeaves();
    }
}
