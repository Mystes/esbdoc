package fi.mystes.esbdoc;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;

/**
 * Created by mystes-am on 26.5.2015.
 * TODO why does this have to suck so much?
 */
public class SAXHandler extends DefaultHandler {

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
            this.diagramBuilder.visited.put(proxyName, "visited");
            this.diagramBuilder.output.append("Title " + proxyName + "\n");
        } // handle endpoint file
        else if (qName.equals("endpoint")) {
            String name = attributes.getValue("name");
            String callable = attributes.getValue("key");
            //handle sequence file
            if (name != null) {
                root = name;
                this.diagramBuilder.visited.put(name, "visited");
                this.diagramBuilder.output.append("Title " + name + "\n");
            } // handle callable endpoint
            else if (callable != null) {
                callEndPointOnDemand(callable);
            } else {
                //System.out.println("Unhandled endpoint type, skipping...");
            }
        } else if (qName.equals("address")) {
            String uriTarget = attributes.getValue("uri");
            if (uriTarget.toLowerCase().contains("proxy")) {
                String proxy = new EndpointURI(attributes.getValue("uri")).getTarget();
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
                this.diagramBuilder.visited.put(sequenceName, "visited");
                this.diagramBuilder.output.append("Title " + sequenceName + "\n");
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
                    service = new EndpointURI(service).getTarget();
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
            this.diagramBuilder.output.append("alt ").append(condition).append("\n");
        } // handle fitler's else
        else if (qName.equals("else")) {
            this.diagramBuilder.output.append("else\n");
        } // handle switch
        else if (qName.equals("switch")) {
            this.diagramBuilder.output.append("alt ").append(attributes.getValue("source")).append("");
            firstCase = true;
        } // handle switch's case
        else if (qName.equals("case")) {
            if (!firstCase) {
                this.diagramBuilder.output.append("else ");
            } else {
                this.diagramBuilder.output.append(" == ");
                firstCase = false;
            }
            this.diagramBuilder.output.append("\"").append(attributes.getValue("regex")).append("\"\n");
        } // handle switch's default
        else if (qName.equals("default")) {
            this.diagramBuilder.output.append("else\n");
        } // handle faultsequence as filter
        else if (qName.equals("faultsequence")) {
            this.diagramBuilder.output.append("alt SOAP fault occurred\n");
        } // handle iterate
        else if (qName.equals("iterate")) {
            this.diagramBuilder.output.append("loop ").append(attributes.getValue("expression")).append("\n");
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
            this.diagramBuilder.output.append("end\n");
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
        this.diagramBuilder.output.append(root).append(" ->+ ").append(name).append(":\n");
        try {
            //in case of recursive sequence call
            if (!this.diagramBuilder.visited.containsKey(name)) {
                String tempName = name + "-1.0.0.xml";
                if (fileExists(tempName, dir)) {
                    diagramBuilder.build(dir + "/" + tempName);
                }
            }

        } catch (Exception e) {
            this.diagramBuilder.output.append(name).append(" ->- ").append(root).append(":\n");
            throw new SAXException(e);
        }
        this.diagramBuilder.output.append(name).append(" ->- ").append(root).append(":\n");
    }

    private boolean fileExists(String name, String dir) {
        File file = new File(this.diagramBuilder.filesHome + "/" + dir + "/" + name);
        return file.exists() && file.isFile();
    }
}
