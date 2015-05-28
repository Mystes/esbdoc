package fi.mystes.esbdoc;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

import static fi.mystes.esbdoc.Element.Type.*;
import static fi.mystes.esbdoc.Element.Attribute.*;
import static fi.mystes.esbdoc.Element.SpringBean.*;
import static fi.mystes.esbdoc.Element.Springproperty.*;

/**
 * Created by mystes-am on 26.5.2015.
 * TODO why does this have to suck so much?
 */
public class SAXHandler extends DefaultHandler {

    private SequenceDiagramBuilder diagramBuilder;

    private String root;
    private String simpleIteratorSplitExpression;
    private String simpleIteratorTarget;
    private boolean firstCase = true;

    private static final String POSITIVE = " ->+ ";
    private static final String NEGATIVE = " ->- ";

    private static final String SEQUENCE_DIRECTORY = "sequences";
    private static final String PROXY_DIRECTORY = "proxy-services";
    private static final String ENDPOINT_DIRECTORY = "endpoints";

    private static final String FILE_SUFFIX = "-1.0.0.xml";

    public SAXHandler(SequenceDiagramBuilder builder) {
        this.diagramBuilder = builder;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        super.startElement(uri, localName, qName, attributes);
        Element element = new Element(qName, attributes);
        if (element.is(PROXY)) {
            String proxyName = element.get(NAME);
            root = proxyName;
            setVisited(proxyName);
            append("Title " + proxyName + "\n");
        }
        if (element.is(ENDPOINT)) {
            String name = element.get(NAME);
            String callable = element.get(KEY);
            if (name != null) {
                root = name;
                setVisited(name);
                append("Title " + name + "\n");
            }
            if (callable != null) {
                callEndPointOnDemand(callable);
            }
        }
        if (element.is(ADDRESS)) {
            String uriTarget = element.get(URI);
            if (uriTarget.toLowerCase().contains("proxy")) {
                String proxy = new EndpointURI(element.get(URI)).getTarget();
                if (proxy != null) {
                    callProxyOnDemand(proxy);
                }
            } else {
                callEndPointOnDemand("\"" + uriTarget + "\"");
            }
        }
        if (element.is(SEQUENCE_TYPE)) {
            String sequenceName = element.get(NAME);
            String callableSequence = element.get(KEY);
            if (sequenceName != null) {
                root = sequenceName;
                setVisited(sequenceName);
                append("Title " + sequenceName + "\n");
            }
            if (callableSequence != null) {
                callSequenceOnDemand(callableSequence);
            }
        }
        if (element.isAnyOf(CUSTOM_CALLOUT, CALLOUT)) {
            String service = element.get(SERVICE_URL);
            if (service != null) {
                if (service.startsWith("http")) {
                    service = new EndpointURI(service).getTarget();
                }
                callProxyOnDemand(service);
            } else {
                service = element.get(ENDPOINT_KEY);
                callEndPointOnDemand(service);
            }
        }
        if (element.is(TARGET)) {
            if (element.has(IN_SEQUENCE)) {
                callSequenceOnDemand(element.get(IN_SEQUENCE));
            }
            if (element.has(OUT_SEQUENCE)) {
                callSequenceOnDemand(element.get(OUT_SEQUENCE));
            }
            if (element.has(SEQUENCE_VALUE)) {
                callSequenceOnDemand(element.get(SEQUENCE_VALUE));
            }
        }
        if (element.is(FILTER)) {
            String condition = element.get(XPATH);
            if (condition == null) {
                condition = element.get(SOURCE) + " == " + element.get(REGEX);
            }
            append("alt ").append(condition).append("\n");
        }
        if (element.is(ELSE)) {
            append("else\n");
        }
        if (element.is(SWITCH)) {
            append("alt ").append(element.get(SOURCE)).append("");
            firstCase = true;
        }
        if (element.is(CASE)) {
            if (!firstCase) {
                append("else ");
            } else {
                append(" == ");
                firstCase = false;
            }
            append("\"").append(element.get(REGEX)).append("\"\n");
        }
        if (element.is(DEFAULT)) {
            append("else\n");
        }
        if (element.is(FAULT_SEQUENCE)) {
            append("alt SOAP fault occurred\n");
        }
        if (element.is(ITERATE)) {
            append("loop ").append(element.get(EXPRESSION)).append("\n");
        }
        if (element.hasSpringProperty(SIMPLE_ITERATOR_SPLIT_EXPRESSION)) {
            simpleIteratorSplitExpression = element.get(VALUE);

        }
        if (element.hasSpringProperty(SIMPLE_ITERATOR_TARGET)) {
            simpleIteratorTarget = element.get(VALUE);
        }
        if (element.isSpringBean(SIMPLE_ITERATOR)) {
            if (simpleIteratorSplitExpression != null && simpleIteratorTarget != null) {
                callOnDemand(simpleIteratorTarget, null);
                simpleIteratorSplitExpression = null;
                simpleIteratorTarget = null;
            }
        }
        if (element.nameContains("store")) {
            callOnDemand(element.get(MESSAGE_STORE), null);
        }
        if (element.is(CLASS)) {
            callOnDemand(element.get(NAME), null);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        Element element = new Element(qName);
        if (element.isAnyOf(ITERATE, SWITCH, FILTER, FAULT_SEQUENCE)) {
            append("end\n");
        }
    }

    private void callSequenceOnDemand(String name) throws SAXException {
        callOnDemand(name, SEQUENCE_DIRECTORY);
    }

    private void callProxyOnDemand(String name) throws SAXException {
        callOnDemand(name, PROXY_DIRECTORY);
    }

    private void callEndPointOnDemand(String name) throws SAXException {
        callOnDemand(name, ENDPOINT_DIRECTORY);
    }

    private void callOnDemand(String name, String dir) throws SAXException {
        addPositiveRelation(root, name);
        try {
            buildDiagramFromFile(name, dir);
        } catch (Exception e) {
            throw new SAXException(e);
        } finally {
            addNegativeRelation(name, root);
        }
    }

    private void buildDiagramFromFile(String fileName, String directory) throws IOException, ParserConfigurationException, SAXException {
        if(isVisited(fileName)){
            return;
        }
        String tempName = fileName + FILE_SUFFIX;
        if (fileExists(tempName, directory)) {
            diagramBuilder.build(directory + "/" + tempName);
        }
    }

    private boolean isVisited(String name){
        //TODO get rid of this stupid diagrambuilder reference
        return this.diagramBuilder.visited.containsKey(name);
    }

    private void setVisited(String item){
        //TODO get rid of this stupid diagrambuilder reference
        this.diagramBuilder.visited.put(item, "visited");
    }

    private StringBuilder append(String diagramItem){
        //TODO get rid of this stupid diagrambuilder reference
        return this.diagramBuilder.output.append(diagramItem);
    }

    private void addPositiveRelation(String from, String to){
        append(relation(from, to, POSITIVE));
    }

    private void addNegativeRelation(String from, String to){
        append(relation(from, to, NEGATIVE));
    }

    private String relation(String from, String to, String type){
        return from + type + to + ":\n";
    }

    private boolean fileExists(String name, String dir) {
        File file = new File(this.diagramBuilder.filesHome + "/" + dir + "/" + name);
        return file.exists() && file.isFile();
    }
}
