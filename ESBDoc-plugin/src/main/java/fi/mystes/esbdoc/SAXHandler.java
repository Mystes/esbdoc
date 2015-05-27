package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

import static fi.mystes.esbdoc.SAXHandler.Element.Type.*;
import static fi.mystes.esbdoc.SAXHandler.Element.Attribute.*;

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

    public static class Element {

        public enum Type {
            PROXY("proxy"),
            ENDPOINT("endpoint"),
            ADDRESS("address"),
            SEQUENCE_TYPE("sequence"),
            CUSTOM_CALLOUT("customcallout"),
            CALLOUT("callout"),
            TARGET("target"),
            FILTER("filter"),
            ELSE("else"),
            SWITCH("switch"),
            CASE("case"),
            DEFAULT("default"),
            FAULT_SEQUENCE("faultsequence"),
            ITERATE("iterate"),
            PROPERTY("property"),
            SPRING("spring"),
            CLASS("class");

            private final String name;

            Type(String name){
                this.name = name;
            }

            public String getName(){
                return this.name;
            }
        }

        public enum Attribute {
            NAME("name"),
            KEY("key"),
            URI("uri"),
            SERVICE_URL("serviceURL"),
            ENDPOINT_KEY("endpointKey"),
            IN_SEQUENCE("inSequence"),
            OUT_SEQUENCE("outSequence"),
            SEQUENCE_VALUE("sequence"),
            XPATH("xpath"),
            SOURCE("source"),
            REGEX("regex"),
            EXPRESSION("expression"),
            VALUE("value"),
            BEAN("bean"),
            MESSAGE_STORE("messageStore");

            private final String name;

            Attribute(String name){
                this.name = name;
            }

            public String getName(){
                return this.name;
            }
        }

        private final String name;
        private final Attributes attributes;

        protected Element(String name){
            this.name = name;
            this.attributes = new AttributesImpl();
        }

        protected Element(String name, Attributes attributes){
            this.name = name;
            this.attributes = attributes;
        }

        protected boolean is(Type type){
            return is(type.getName().toLowerCase());
        }

        protected boolean is(String type){
            return StringUtils.equals(this.name.toLowerCase(), type.toLowerCase());
        }

        protected boolean nameContains(String partOfname){
            return StringUtils.contains(this.name.toLowerCase(), partOfname.toLowerCase());
        }

        protected boolean isNot(Type type){
            return !is(type);
        }

        protected boolean isAnyOf(Type... types){
            for(Type type : types){
                if(this.is(type)){
                    return true;
                }
            }
            return false;
        }

        protected boolean isAnyOf(String... types){
            for(String type : types){
                if(this.is(type)){
                    return true;
                }
            }
            return false;
        }

        protected boolean isNoneOf(String... types){
            return !isAnyOf(types);
        }

        protected boolean has(Attribute attribute){
            return has(attribute.getName());
        }

        protected boolean has(String attributeName){
            return StringUtils.isNotEmpty(this.get(attributeName));
        }

        protected boolean doesNotHave(Attribute attribute){
            return !has(attribute);
        }

        protected boolean doesNotHave(String attributeName){
            return !has(attributeName);
        }

        protected String get(Attribute attribute){
            return get(attribute.getName());
        }

        protected String get(String attributeName){
            return this.getAttributes().getValue(attributeName);
        }

        protected boolean valueEquals(Attribute target, String expectedValue){
            if(doesNotHave(target)){
                return false;
            }
            String actualValue = get(target);
            return StringUtils.equalsIgnoreCase(expectedValue, actualValue);
        }

        protected String getName(){
            return this.name;
        }

        protected Attributes getAttributes(){
            return this.attributes;
        }

    }

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
            this.diagramBuilder.visited.put(proxyName, "visited");
            append("Title " + proxyName + "\n");
        }
        if (element.is(ENDPOINT)) {
            String name = element.get(NAME);
            String callable = element.get(KEY);
            if (name != null) {
                root = name;
                this.diagramBuilder.visited.put(name, "visited");
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
                this.diagramBuilder.visited.put(sequenceName, "visited");
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
        if (element.is(PROPERTY) && element.has(NAME)) {
            if (element.valueEquals(NAME, "simpleiterator.splitexpression")) {
                simpleIteratorSplitExpression = element.get(VALUE);

            } else if (element.valueEquals(NAME, "simpleiterator.target")) {
                simpleIteratorTarget = element.get(VALUE);
            }
        }
        if (element.is(SPRING) && element.valueEquals(BEAN, "simpleiterator")) {
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
        return this.diagramBuilder.visited.containsKey(name);
    }

    private void addPositiveRelation(String from, String to){
        append(relation(from, to, POSITIVE));
    }

    private void addNegativeRelation(String from, String to){
        append(relation(from, to, NEGATIVE));
    }

    private StringBuilder append(String diagramItem){
        return this.diagramBuilder.output.append(diagramItem);
    }

    private String relation(String from, String to, String type){
        return from + type + to + ":\n";
    }

    private boolean fileExists(String name, String dir) {
        File file = new File(this.diagramBuilder.filesHome + "/" + dir + "/" + name);
        return file.exists() && file.isFile();
    }
}
