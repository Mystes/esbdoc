package fi.mystes.esbdoc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SequenceDiagramBuilder {
    private static Log log = LogFactory.getLog(SequenceDiagramBuilder.class);

    public static final String FILE_SUFFIX = "-seq.json";
    private static final String PATH_TO_ESB = "/home/kreshnikg/Applications/wso2esb-4.5.1"; //FIXME that's user-specific
    private static final String RELATIVE_PATH_TO_CONFIGURATION_FILES = "/repository/deployment/server/synapse-configs/default/";

    private static SequenceDiagramBuilder instance = null;
    private String filesHome;
    private HashMap<String, SequenceItem> sequenceItemMap = new HashMap();
    private Map<String, String> visited = new HashMap<String, String>();
    private StringBuilder output = null;

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

    public static void main(String[] args) {
        //TODO What information does magic argument number seven carry and what about the other args?
        String result = new SequenceDiagramBuilder(PATH_TO_ESB).buildPipe(args[7]);
        log.info(result);
    }

    private SAXParser parser() throws SAXException, ParserConfigurationException{
        log.debug("Instantiating a new SAX Parser...");
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
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

    private SequenceDiagramBuilder build(String file) throws SAXException, IOException, ParserConfigurationException {
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

    class SAXHandler extends DefaultHandler {

        private String root;
        private SequenceDiagramBuilder diagramBuilder;
        private String simpleIteratorSplitExpression;
        private String simpleIteratorTarget;
        private boolean firstCase = true;

        public SAXHandler(SequenceDiagramBuilder builder) {
            this.diagramBuilder = builder;

        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            super.startElement(uri, localName, qName, attributes);
            qName = qName.toLowerCase();
            // handle proxy file
            if (qName.equals("proxy")) {
                String proxyName = attributes.getValue("name");
                root = proxyName;
                visited.put(proxyName, "visited");
                output.append("Title " + proxyName + "\n");
            } // handle endpoint file
            else if (qName.equals("endpoint")) {
                String name = attributes.getValue("name");
                String callable = attributes.getValue("key");
                //handle sequence file
                if (name != null) {
                    root = name;
                    visited.put(name, "visited");
                    output.append("Title " + name + "\n");
                } // handle callable endpoint
                else if (callable != null) {
                    callEndPointOnDemand(callable);
                } else {
                    //System.out.println("Unhandled endpoint type, skipping...");
                }
            } else if (qName.equals("address")) {
                String uriTarget = attributes.getValue("uri");
                if (uriTarget.toLowerCase().contains("proxy")) {
                    String proxy = getTargetFromEndPointURI(attributes.getValue("uri"));
                    if (proxy != null) {
                        callProxyOnDemand(proxy);
                    } else {
                        //System.out.println("Unhandled uri: " + attributes.getValue("uri"));
                    }
                } else {
                    callEndPointOnDemand("\"" + uriTarget + "\"");
                }
            } // handle sequence
            else if (qName.equals("sequence")) {
                String sequenceName = attributes.getValue("name");
                String callableSequence = attributes.getValue("key");
                //handle sequence file
                if (sequenceName != null) {
                    root = sequenceName;
                    visited.put(sequenceName, "visited");
                    output.append("Title " + sequenceName + "\n");
                } // handle callable sequence
                else if (callableSequence != null) {
                    callSequenceOnDemand(callableSequence);
                } // anonymous/in-line sequence needs no handling
                else {
                    //System.out.println("Unhandled sequence type, skipping...");
                }
            } // handle callable proxy
            else if (!(!qName.equals("customcallout") && !qName.equals("callout"))) {
                String service = attributes.getValue("serviceURL");
                if (service != null) {
                    if (service.startsWith("http")) {
                        // This should be URL so  resolve  target
                        service = getUrlTargetNode(service);
                    }
                    callProxyOnDemand(service);
                } else {
                    service = attributes.getValue("endpointKey");
                    callEndPointOnDemand(service);
                }
            } else if (qName.equals("target")) {
                // handle inSequennce/outSequence attributes
                // target of proxy
                if (attributes.getValue("inSequence") != null) {
                    callSequenceOnDemand(attributes.getValue("inSequence"));
                }
                //target of proxy
                if (attributes.getValue("outSequence") != null) {
                    callSequenceOnDemand(attributes.getValue("outSequence"));
                }
                // target of iterate mediator
                if (attributes.getValue("sequence") != null) {
                    callSequenceOnDemand(attributes.getValue("sequence"));
                }
            } // handle filter
            else if (qName.equals("filter")) {
                String condition = attributes.getValue("xpath");
                if (condition == null) {
                    condition = attributes.getValue("source") + " == " + attributes.getValue("regex");
                }
                output.append("alt ").append(condition).append("\n");
            } // handle fitler's else
            else if (qName.equals("else")) {
                output.append("else\n");
            } // handle switch
            else if (qName.equals("switch")) {
                output.append("alt ").append(attributes.getValue("source")).append("");
                firstCase = true;
            } // handle switch's case
            else if (qName.equals("case")) {
                if (!firstCase) {
                    output.append("else ");
                } else {
                    output.append(" == ");
                    firstCase = false;
                }
                output.append("\"").append(attributes.getValue("regex")).append("\"\n");
            } // handle switch's default
            else if (qName.equals("default")) {
                output.append("else\n");
            } // handle faultsequence as filter
            else if (qName.equals("faultsequence")) {
                output.append("alt SOAP fault occurred\n");
            } // handle iterate
            else if (qName.equals("iterate")) {
                output.append("loop ").append(attributes.getValue("expression")).append("\n");
            } // handle simple iterator (custom)
            else if (qName.equals("property") && attributes.getValue("name") != null) {
                if (attributes.getValue("name").toLowerCase().equals("simpleiterator.splitexpression")) {
                    simpleIteratorSplitExpression = attributes.getValue("value");

                } else if (attributes.getValue("name").toLowerCase().equals("simpleiterator.target")) {
                    simpleIteratorTarget = attributes.getValue("value");
                }
            } else if (qName.equals("spring") && attributes.getValue("bean") != null && attributes.getValue("bean").toLowerCase().equals("simpleiterator")) {
                if (simpleIteratorSplitExpression != null && simpleIteratorTarget != null) {
                    callOnDemand(simpleIteratorTarget, null);
                    simpleIteratorSplitExpression = null;
                    simpleIteratorTarget = null;
                }
            } // handle callable store
            else if (qName.toLowerCase().contains("store")) {
                callOnDemand(attributes.getValue("messageStore"), null);
            } //handle callable class
            else if (qName.equals("class")) {
                callOnDemand(attributes.getValue("name"), null);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(uri, localName, qName);
            qName = qName.toLowerCase();
            if (!(!qName.equals("iterate") && !qName.equals("switch") && !qName.equals("filter") && !qName.equals("faultsequence"))) {
                output.append("end\n");
            }
        }

        private String getUrlTargetNode(String targ) {
            try {
                String target = null;
                // This should be URL so  resolve  target
                URL url = new URL(targ);
                target = url.getFile();
                target = target.substring(target.lastIndexOf("/") + 1, target.length());
                //If it is  still on  notation xxx.yyy, use last part
                if (target.lastIndexOf(".") > 0) {
                    int start = target.lastIndexOf(".") + 1;
                    int end = target.length();
                    if (target.toLowerCase().contains("endpoint")) {
                        end = start - 1;
                        start = 0;
                    }
                    target = target.substring(start, end);
                }
                return target;
            } catch (MalformedURLException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
                return null;
            }
        }

        private void callSequenceOnDemand(String name) throws SAXException {
            callOnDemand(name, "sequences");
        }

        private void callProxyOnDemand(String name) throws SAXException {
            callOnDemand(name, "proxy-services");
        }

        private void callEndPointOnDemand(String name) throws SAXException {
            callOnDemand(name, "endpoints");
        }

        private void callOnDemand(String name, String dir) throws SAXException {
            output.append(root).append(" ->+ ").append(name).append(":\n");
            try {
                //in case of recursive sequence call
                if (!visited.containsKey(name)) {
                    String tempName = name + "-1.0.0.xml";
                    if (fileExists(tempName, dir)) {
                        diagramBuilder.build(dir + "/" + tempName);
                    }
                }

            } catch (Exception e) {
                output.append(name).append(" ->- ").append(root).append(":\n");
                throw new SAXException(e);
            }
            output.append(name).append(" ->- ").append(root).append(":\n");
        }

        private boolean fileExists(String name, String dir) {
            File file = new File(filesHome + "/" + dir + "/" + name);
            return file.exists() && file.isFile();
        }

        private String getTargetFromEndPointURI(String uri) {
            //uri="http://localhost:8280/services/DeleteMacoContentsFromSSCOByQueueProxy"
            //uri="jms:/GetMacoContentProductKeyQueueProxy?transport.jms.ConnectionFactoryJ...

            if (uri.startsWith("http")) {
                return getUrlTargetNode(uri);
            }

            if (uri.startsWith("jms")) {
                return uri.split("\\?")[0].split("/")[1];
            }

            return null;
        }
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
        FileOutputStream fis = new FileOutputStream(new File(outputFilename + FILE_SUFFIX));
        JsonFactory factory = new JsonFactory();
        return factory.createGenerator(fis);
    }

    private String printDependenciesRecursively(SequenceItemParameters params) throws IOException {
        String key = params.getKey();
        String parent = params.getParent();

        if (params.containsCircularDependencies()) {
            return params.toString();
        }

        if (parent != null) {
            params.addDependency(parent, key);
        }

        params.addHandledNode(key);
        populateLeaves(params);

        if (parent != null) {
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

    private class SequenceItemParameters {
        private final StringBuilder stringBuilder = new StringBuilder();
        private final List<String> handledNodeList = new ArrayList();

        private String key;
        private String parent = null;

        public SequenceItemParameters(String key){
            this.key = key;
        }

        private StringBuilder getStringBuilder(){
            return this.stringBuilder;
        }

        private List<String> getHandledNodeList(){
            return this.handledNodeList;
        }

        public String getKey(){
            return this.key;
        }

        public void setKey(String key){
            this.key = key;
        }

        public String getParent(){
            return this.parent;
        }

        public void setParent(String parent){
            this.parent = parent;
        }

        public String toString(){
            return this.getStringBuilder().toString();
        }

        public boolean containsCircularDependencies(){
            return this.getHandledNodeList().contains(this.getKey());
        }

        public void addDependency(String from, String to){
            this.getStringBuilder().append(dependency(from, to));
        }

        private String dependency(String from, String to){
            return from + "->" + to + ":\n";
        }

        public void addHandledNode(String key){
            this.getHandledNodeList().add(key);
        }

    }

}
