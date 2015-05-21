package fi.mystes.esbdoc;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import net.sf.saxon.s9api.SaxonApiException;
import org.apache.commons.lang3.StringEscapeUtils;
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

    private static final String PATH_TO_ESB = "/home/kreshnikg/Applications/wso2esb-4.5.1"; //FIXME that's user-specific
    private static final String RELATIVE_PATH_TO_CONFIGURATION_FILES = "/repository/deployment/server/synapse-configs/default/";

    private static SequenceDiagramBuilder instance = null;
    private String filesHome;
    private HashMap<String, SequenceItem> parsed = new HashMap();
    private Map<String, String> visited = new HashMap();
    private StringBuilder output = null;

    private SequenceDiagramBuilder(String wso2Home) {
        filesHome = wso2Home + RELATIVE_PATH_TO_CONFIGURATION_FILES;
        visited = new HashMap<String, String>();
    }

    private SequenceDiagramBuilder() {
        visited = new HashMap<String, String>();
    }

    public static SequenceDiagramBuilder instance() {
        if (null == instance) {
            log.info("Creating a new instance of the SequenceDiagramBuilder.");
            instance = new SequenceDiagramBuilder();
        }
        return instance;
    }

    private SequenceDiagramBuilder build(String file) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory parserFactor = SAXParserFactory.newInstance();
        SAXParser parser = parserFactor.newSAXParser();
        parser.parse(new FileInputStream(filesHome + file), getNewHandler());
        return this;
    }

    private SequenceDiagramBuilder build(InputStream is) throws SAXException, IOException, ParserConfigurationException {
        SAXParserFactory parserFactor = SAXParserFactory.newInstance();
        SAXParser parser = parserFactor.newSAXParser();
        parser.parse(is, getNewHandler());
        return this;
    }

    public String buildPipe(String file) {
        visited = new HashMap<String, String>();
        output = new StringBuilder();
        try {
            build(file);
            return output.toString();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String buildPipe(InputStream is) {
        visited = new HashMap<String, String>();
        output = new StringBuilder();
        try {
            build(is);
            if (output.length() > 6) {
                // Try to resolve  name and use it as a key
                SequenceItem item = create_callSeqStructure(output.toString());
                // Save item into hashmap for further processing.                
                getParsed().put(item.getName(), item);
            }
            return output.toString();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SAXHandler getNewHandler() {
        return new SAXHandler(this);
    }

    /**
     * Main set up environment and generates all graphs. It uses WSO2_HOME as a
     * starting point where to find proxies and sequences.
     *
     * @param args
     * @throws SaxonApiException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    public static void main(String[] args) {

        String result = new SequenceDiagramBuilder(PATH_TO_ESB).buildPipe(args[7]);
        System.out.println(result);
    }

    /**
     * @return the parsed
     */
    public HashMap<String, SequenceItem> getParsed() {
        return parsed;
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
            } else if (uri.startsWith("jms")) {
                return uri.split("\\?")[0].split("/")[1];
            }
            return null;
        }
    }

    public SequenceItem create_callSeqStructure(String source) {
        String name = null;
        if (!source.isEmpty()) {
            SequenceItem item = new SequenceItem(source);
            return item;
        } else {
            return null;
        }
    }
    /*
     Write call graph using following json format:
    
     {
     "models": {
     "model": [
     {
     "name": "Material provisioning",
     "description": "
     Material provisioning covers the provisioning of school and user materials in D2L according to access rights data in UMS DB. The actual material that is provisioned is maintained in D2L MaCo (Master Course Offering) structure. UMS holds the information of access rights of users and schools to UMS packages, and the packages contain the actual MaCo rights. The actual material provisioning copies the MaCo workspace contents to School Specific Course Offerings (SSCOs) or User Specific Course Offerings (USCOs) in D2L. The materials that ESB provisions have two main types
     Method materials (a.k.a. Veloituksettomat materiaalit): These are additional digital contents to physical book series bought by schools. E.g. a school that purchases \"Matikka 3\" books get the method materials to Matikka 3 in D2L without extra charge. The method materials are only available to schools
     Licence materials (a.k.a. e-materials a.k.a. lisenssimateriaalit): These are digital contents that schools and individual users can purchase in Sanoma Pro portal. 
     Material access rights are updated to UMS from several sources:
     Method rights are maintained in SAP and provisioned to UMS and Liferay 
     License materials for webshop purchases and access rights added by CS using Liferay CS UI are provisioned to UMS by Liferay 
     It is also possible to add access rights to both types of materials directly via UMSAdmin 
     ",
     "sequence": "SAP->ESB: BP02 update
     note left of ESB: /ratkoo/api/bpOutbound
     ESB->Liferay: BP02 update
     UMS->ESB: poll method right changes
     note right of ESB: ESB proxies BP02 messages to Liferay\\n note right of ESB: ESB stores method data locally.\\n note right of ESB: Methods of main school and side branches are synced\\n note right of ESB: (synced method data is also sent to Liferay).\"
     "
     },
     {
     "name": "Material provisioning flow - method access rights from SAP to UMS/Liferay",
     "description": "
     ",
     "sequence": "SAP->ESB: BP02 update
     note left of ESB: /ratkoo/api/bpOutbound
     ESB->Liferay: BP02 update
     UMS->ESB: poll method right changes
     note right of ESB: ESB proxies BP02 messages to Liferay\\n note right of ESB: ESB stores method data locally.\\n note right of ESB: Methods of main school and side branches are synced\\n note right of ESB: (synced method data is also sent to Liferay).\"
     "
     }
     ]
     }
     }            */

    public void writeOutputFiles(String outputFilename) throws FileNotFoundException, IOException {
        new File(outputFilename).getParentFile().mkdirs();
        ArrayList<String> handledNodeList = new ArrayList();

        System.out.println("Writing out:" + outputFilename);
        FileOutputStream fis = null;
        fis = new FileOutputStream(new File(outputFilename + "-seq.json"));

        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = factory.createGenerator(fis);
        generator.writeStartObject();
        generator.writeObjectFieldStart("models");
        generator.writeArrayFieldStart("sequence-models");

        // write H
        Set<String> keys = parsed.keySet();
        Iterator<String> it = keys.iterator();
        while (it.hasNext()) {
            String current = it.next();
            System.out.println("Jasonifying:" + current);
            generator.writeStartObject();
            generator.writeStringField("name", current);
            generator.writeStringField("description", "");

            StringBuilder seqString = new StringBuilder();
            String block = printDependencies(seqString, current, null, 0, handledNodeList, parsed);
//            System.out.println(StringEscapeUtils.escapeJson(block));
            generator.writeStringField("sequence", StringEscapeUtils.escapeJson(block));
            generator.writeEndObject();
            handledNodeList.clear();
        }
        generator.writeEndArray();
        generator.writeEndObject();
        generator.close();
    }

    /*
     public void writeOutputFiles(String outputFilename) throws FileNotFoundException, IOException {
     new File(outputFilename).getParentFile().mkdirs();
     ArrayList<String> handledNodeList = new ArrayList();

     System.out.println("Writing out:" + outputFilename);
     FileOutputStream fis = null;
     fis = new FileOutputStream(new File(outputFilename + "-seq.json"));
     // write Header
     fis.write("{\n\"models\": {\n".getBytes("UTF-8"));
     fis.write("   {\n      \"model\": [\n".getBytes("UTF-8"));
     Set<String> keys = parsed.keySet();
     Iterator<String> it = keys.iterator();
     while (it.hasNext()) {
     String current = it.next();
     System.out.println("----------------" + current + "---------------------");
            
     // Single object 
     fis.write("        {\n".getBytes("UTF-8"));
     fis.write("            \"name\": \"".getBytes("UTF-8"));
     fis.write(current.getBytes("UTF-8"));
     fis.write("\",          \n".getBytes("UTF-8"));

     StringBuilder seqString = new StringBuilder();
     String block = printDependencies(seqString, it.next(), null, 0, handledNodeList, parsed);
     seqString = null;
     System.out.println(StringEscapeUtils.escapeJson(block));
     fis.write("            \"sequence\": \"".getBytes("UTF-8"));

     fis.write(StringEscapeUtils.escapeJson(block).getBytes("UTF-8"));
     fis.write("\"\n        }".getBytes("UTF-8"));

     block = null;
     handledNodeList.clear();
     if(it.hasNext()){
     fis.write(",\n".getBytes("UTF-8"));                
     }
     else 
     fis.write("\n".getBytes("UTF-8"));                
     }
     fis.write("        ]\n}".getBytes("UTF-8"));        
     fis.close();
     }
            
     */
    /**
     * Recursively prints out each dependency graph for given proxy or sequency.
     * It uses depth-first style handling.
     *
     * @param outputStr StringBuilder where it writes out current dependency
     * block
     * @param source name of the current node
     * @param parent name of the parent node. If parent is null, we have whole
     * new proxy or sequence
     * @param indent just count of how many  <space> to print before node
     * @param handledNodeList list which collects information whether this node
     * is already printed. It removes circular references.
     * @param nodeDependencies list which contains all known nodes. Function
     * uses <source> as a key when getting all leaves that given source has.
     * Then it proceeds each leave similarly.
     * @throws FileNotFoundException
     * @throws IOException
     */
    private String printDependencies(StringBuilder outputStr, String source, String parent, int indent, List<String> handledNodeList, Map<String, SequenceItem> nodeDependencies) throws FileNotFoundException, IOException {
        List<String> leaves = null;
        SequenceItem item = nodeDependencies.get(source);
        if (item != null) {
            leaves = item.getLeaves();
            //     System.out.println("Handling:"+item.getPayload());
        } else {
            // Termination
//            System.out.println("Missing leaves for: <"+source+"> ");
        }
        // Just make sure there is no  circular references
        if (handledNodeList.contains(source)) {
            return outputStr.toString(); // This node and it's children is allready printed out.
        }
        handledNodeList.add(source);

        if (parent != null) {
            /* removed indentation for http://bramp.github.io/js-sequence-diagrams/ 
             for (int x = 0; x < indent; x++) {
             outputStr.append(" ");
             }
             */
            outputStr.append(parent + "->" + source + ":\n");
        } else {
//            outputStr.append("title " + source + "\n");
            // Main-level
        }
        if (leaves != null) {
            for (String s : leaves) {
                printDependencies(outputStr, s, source, indent + 1, handledNodeList, nodeDependencies);
            }
        }
        if (parent != null) {
            outputStr.append(source + "->" + parent + ":\n");
        }

        return outputStr.toString(); // This node and it's children is allready printed out.
    }

}
