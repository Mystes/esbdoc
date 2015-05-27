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

    private static final String POSITIVE = " ->+ ";
    private static final String NEGATIVE = " ->- ";

    public SAXHandler(SequenceDiagramBuilder builder) {
        this.diagramBuilder = builder;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        qName = qName.toLowerCase();
        if (qName.equals("proxy")) {
            String proxyName = attributes.getValue("name");
            root = proxyName;
            this.diagramBuilder.visited.put(proxyName, "visited");
            this.diagramBuilder.output.append("Title " + proxyName + "\n");
        }
        if (qName.equals("endpoint")) {
            String name = attributes.getValue("name");
            String callable = attributes.getValue("key");
            if (name != null) {
                root = name;
                this.diagramBuilder.visited.put(name, "visited");
                this.diagramBuilder.output.append("Title " + name + "\n");
            }
            if (callable != null) {
                callEndPointOnDemand(callable);
            }
        } else if (qName.equals("address")) {
            String uriTarget = attributes.getValue("uri");
            if (uriTarget.toLowerCase().contains("proxy")) {
                String proxy = new EndpointURI(attributes.getValue("uri")).getTarget();
                if (proxy != null) {
                    callProxyOnDemand(proxy);
                }
            } else {
                callEndPointOnDemand("\"" + uriTarget + "\"");
            }
        }
        if (qName.equals("sequence")) {
            String sequenceName = attributes.getValue("name");
            String callableSequence = attributes.getValue("key");
            if (sequenceName != null) {
                root = sequenceName;
                this.diagramBuilder.visited.put(sequenceName, "visited");
                this.diagramBuilder.output.append("Title " + sequenceName + "\n");
            }
            if (callableSequence != null) {
                callSequenceOnDemand(callableSequence);
            }
        }
        else if (!(!qName.equals("customcallout") && !qName.equals("callout"))) {
            String service = attributes.getValue("serviceURL");
            if (service != null) {
                if (service.startsWith("http")) {
                    service = new EndpointURI(service).getTarget();
                }
                callProxyOnDemand(service);
            } else {
                service = attributes.getValue("endpointKey");
                callEndPointOnDemand(service);
            }
        } else if (qName.equals("target")) {
            if (attributes.getValue("inSequence") != null) {
                callSequenceOnDemand(attributes.getValue("inSequence"));
            }
            if (attributes.getValue("outSequence") != null) {
                callSequenceOnDemand(attributes.getValue("outSequence"));
            }
            if (attributes.getValue("sequence") != null) {
                callSequenceOnDemand(attributes.getValue("sequence"));
            }
        }
        if (qName.equals("filter")) {
            String condition = attributes.getValue("xpath");
            if (condition == null) {
                condition = attributes.getValue("source") + " == " + attributes.getValue("regex");
            }
            this.diagramBuilder.output.append("alt ").append(condition).append("\n");
        }
        if (qName.equals("else")) {
            this.diagramBuilder.output.append("else\n");
        }
        if (qName.equals("switch")) {
            this.diagramBuilder.output.append("alt ").append(attributes.getValue("source")).append("");
            firstCase = true;
        }
        if (qName.equals("case")) {
            if (!firstCase) {
                this.diagramBuilder.output.append("else ");
            } else {
                this.diagramBuilder.output.append(" == ");
                firstCase = false;
            }
            this.diagramBuilder.output.append("\"").append(attributes.getValue("regex")).append("\"\n");
        }
        if (qName.equals("default")) {
            this.diagramBuilder.output.append("else\n");
        }
        if (qName.equals("faultsequence")) {
            this.diagramBuilder.output.append("alt SOAP fault occurred\n");
        }
        if (qName.equals("iterate")) {
            this.diagramBuilder.output.append("loop ").append(attributes.getValue("expression")).append("\n");
        }
        if (qName.equals("property") && attributes.getValue("name") != null) {
            if (attributes.getValue("name").toLowerCase().equals("simpleiterator.splitexpression")) {
                simpleIteratorSplitExpression = attributes.getValue("value");

            } else if (attributes.getValue("name").toLowerCase().equals("simpleiterator.target")) {
                simpleIteratorTarget = attributes.getValue("value");
            }
        }
        if (qName.equals("spring") && attributes.getValue("bean") != null && attributes.getValue("bean").toLowerCase().equals("simpleiterator")) {
            if (simpleIteratorSplitExpression != null && simpleIteratorTarget != null) {
                callOnDemand(simpleIteratorTarget, null);
                simpleIteratorSplitExpression = null;
                simpleIteratorTarget = null;
            }
        }
        if (qName.toLowerCase().contains("store")) {
            callOnDemand(attributes.getValue("messageStore"), null);
        }
        if (qName.equals("class")) {
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
        addPositiveRelation(root, name);
        try {
            if (isNotVisited(name)) {
                String tempName = name + "-1.0.0.xml";
                if (fileExists(tempName, dir)) {
                    diagramBuilder.build(dir + "/" + tempName);
                }
            }

        } catch (Exception e) {
            throw new SAXException(e);
        } finally {
            addNegativeRelation(name, root);
        }
    }

    private boolean isNotVisited(String name){
        return !isVisited(name);
    }

    private boolean isVisited(String name){
        return this.diagramBuilder.visited.containsKey(name);
    }

    private void addPositiveRelation(String from, String to){
        this.diagramBuilder.output.append(relation(from, to, POSITIVE));
    }

    private void addNegativeRelation(String from, String to){
        this.diagramBuilder.output.append(relation(from, to, NEGATIVE));
    }

    private String relation(String from, String to, String type){
        return from + type + to + ":\n";
    }

    private boolean fileExists(String name, String dir) {
        File file = new File(this.diagramBuilder.filesHome + "/" + dir + "/" + name);
        return file.exists() && file.isFile();
    }
}
