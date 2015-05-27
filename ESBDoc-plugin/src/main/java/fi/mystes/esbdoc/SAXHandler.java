package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;

import static fi.mystes.esbdoc.SAXHandler.Element.Type.PROXY;
import static fi.mystes.esbdoc.SAXHandler.Element.Value.*;

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

    public static class Element {

        public enum Type {
            PROXY("proxy");

            private final String name;

            Type(String name){
                this.name = name;
            }

            public String getName(){
                return this.name;
            }
        }

        public enum Value {
            NAME("name");

            private final String name;

            Value(String name){
                this.name = name;
            }

            public String getName(){
                return this.name;
            }
        }

        private final String name;
        private final Attributes attributes;

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

        protected boolean has(String attributeName){
            return StringUtils.isNotEmpty(this.get(attributeName));
        }

        protected boolean doesNotHave(String attributeName){
            return !has(attributeName);
        }

        protected String get(Value value){
            return get(value.getName());
        }

        protected String get(String attributeName){
            return this.getAttributes().getValue(attributeName);
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
            this.diagramBuilder.output.append("Title " + proxyName + "\n");
        }
        if (element.is("endpoint")) {
            String name = element.get("name");
            String callable = element.get("key");
            if (name != null) {
                root = name;
                this.diagramBuilder.visited.put(name, "visited");
                this.diagramBuilder.output.append("Title " + name + "\n");
            }
            if (callable != null) {
                callEndPointOnDemand(callable);
            }
        }
        if (element.is("address")) {
            String uriTarget = element.get("uri");
            if (uriTarget.toLowerCase().contains("proxy")) {
                String proxy = new EndpointURI(element.get("uri")).getTarget();
                if (proxy != null) {
                    callProxyOnDemand(proxy);
                }
            } else {
                callEndPointOnDemand("\"" + uriTarget + "\"");
            }
        }
        if (element.is("sequence")) {
            String sequenceName = element.get("name");
            String callableSequence = element.get("key");
            if (sequenceName != null) {
                root = sequenceName;
                this.diagramBuilder.visited.put(sequenceName, "visited");
                this.diagramBuilder.output.append("Title " + sequenceName + "\n");
            }
            if (callableSequence != null) {
                callSequenceOnDemand(callableSequence);
            }
        }
        if (element.isAnyOf("customcallout", "callout")) {
            String service = element.get("serviceURL");
            if (service != null) {
                if (service.startsWith("http")) {
                    service = new EndpointURI(service).getTarget();
                }
                callProxyOnDemand(service);
            } else {
                service = element.get("endpointKey");
                callEndPointOnDemand(service);
            }
        }
        if (element.is("target")) {
            if (element.has("inSequence")) {
                callSequenceOnDemand(element.get("inSequence"));
            }
            if (attributes.getValue("outSequence") != null) {
                callSequenceOnDemand(element.get("outSequence"));
            }
            if (attributes.getValue("sequence") != null) {
                callSequenceOnDemand(element.get("sequence"));
            }
        }
        if (element.is("filter")) {
            String condition = element.get("xpath");
            if (condition == null) {
                condition = element.get("source") + " == " + element.get("regex");
            }
            this.diagramBuilder.output.append("alt ").append(condition).append("\n");
        }
        if (element.is("else")) {
            this.diagramBuilder.output.append("else\n");
        }
        if (element.is("switch")) {
            this.diagramBuilder.output.append("alt ").append(element.get("source")).append("");
            firstCase = true;
        }
        if (element.is("case")) {
            if (!firstCase) {
                this.diagramBuilder.output.append("else ");
            } else {
                this.diagramBuilder.output.append(" == ");
                firstCase = false;
            }
            this.diagramBuilder.output.append("\"").append(element.get("regex")).append("\"\n");
        }
        if (element.is("default")) {
            this.diagramBuilder.output.append("else\n");
        }
        if (element.is("faultsequence")) {
            this.diagramBuilder.output.append("alt SOAP fault occurred\n");
        }
        if (element.is("iterate")) {
            this.diagramBuilder.output.append("loop ").append(element.get("expression")).append("\n");
        }
        if (element.is("property") && element.has("name")) {
            if (element.get("name").toLowerCase().equals("simpleiterator.splitexpression")) {
                simpleIteratorSplitExpression = element.get("value");

            } else if (element.get("name").toLowerCase().equals("simpleiterator.target")) {
                simpleIteratorTarget = element.get("value");
            }
        }
        if (element.is("spring") && element.has("bean") && element.get("bean").toLowerCase().equals("simpleiterator")) {
            if (simpleIteratorSplitExpression != null && simpleIteratorTarget != null) {
                callOnDemand(simpleIteratorTarget, null);
                simpleIteratorSplitExpression = null;
                simpleIteratorTarget = null;
            }
        }
        if (element.nameContains("store")) {
            callOnDemand(element.get("messageStore"), null);
        }
        if (element.is("class")) {
            callOnDemand(element.get("name"), null);
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
            if(isVisited(name)){
                return;
            }
            String tempName = name + "-1.0.0.xml";
            if (fileExists(tempName, dir)) {
                diagramBuilder.build(dir + "/" + tempName);
            }

        } catch (Exception e) {
            throw new SAXException(e);
        } finally {
            addNegativeRelation(name, root);
        }
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
