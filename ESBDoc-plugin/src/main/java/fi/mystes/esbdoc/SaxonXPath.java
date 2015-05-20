package fi.mystes.esbdoc;

import net.sf.saxon.s9api.*;
import org.apache.commons.vfs2.FileObject;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mystes-am on 20.5.2015.
 */
public class SaxonXPath {

    public static final Processor PROCESSOR = new Processor(false);
    public static final DocumentBuilder BUILDER = PROCESSOR.newDocumentBuilder();
    public static final XPathCompiler COMPILER = PROCESSOR.newXPathCompiler();

    static {
        COMPILER.declareNamespace(Constants.SYNAPSE_NAMESPACE.PREFIX, Constants.SYNAPSE_NAMESPACE.URI);
        COMPILER.declareNamespace(Constants.SOAPUI_CONFIG_NAMESPACE.PREFIX, Constants.SOAPUI_CONFIG_NAMESPACE.URI);
    }

    public static Builder apply(String thisXpath){
        return new Builder().using(thisXpath);
    }

    public static Builder apply(XPathSelector thisXpath){
        return new Builder().using(thisXpath);
    }

    static class Builder {
        private Executor executor;

        private Builder(){
            this.executor = new Executor();
        }

        private Builder using(String thisXpath) {
            this.executor.xpathString = thisXpath;
            return this;
        }

        private Builder using(XPathSelector thisXpath) {
            this.executor.xpathSelector = thisXpath;
            return this;
        }

        public Executor to(FileObject xmlFo) throws SaxonApiException, IOException {
            this.executor.node = Helper.getNodeFromFileObject(xmlFo);
            return this.executor;
        }

        public Executor to(XdmNode thisNode) {
            this.executor.node = thisNode;
            return this.executor;
        }
    }

    static class Executor {
        private String xpathString;
        private XPathSelector xpathSelector;
        private XdmNode node;


        public XdmItem andReturnAnXdmItem() throws SaxonApiException {
            XPathSelector selector = getSelector();
            selector.setContextItem(node);
            return selector.evaluateSingle();
        }

        public XdmValue andReturnAnXdmValue() throws SaxonApiException {
            XPathSelector selector = getSelector();
            selector.setContextItem(node);
            return selector.evaluate();
        }

        public <T> T andReturnA(Class<T> type) throws SaxonApiException {
            XdmItem item = andReturnAnXdmItem();
            return Helper.convertXdmItemToType(item, type);
        }

        public <T> Set<T> andReturnASetOf(Class<?> type) throws SaxonApiException {
            XdmValue xdmValue = andReturnAnXdmValue();
            Set<T> resultSet = new HashSet<T>();
            for(XdmItem item : xdmValue){
                resultSet.add((T) Helper.convertXdmItemToType(item, type));
            }
            return resultSet;
        }

        private XPathSelector getSelector() throws SaxonApiException{
            if(null == this.xpathSelector){
                this.xpathSelector = load(this.xpathString);
            }
            return this.xpathSelector;
        }

        private XPathExecutable compile(String xpath) throws SaxonApiException {
            return COMPILER.compile(xpath);
        }

        private XPathSelector load(String xpath) throws SaxonApiException {
            return compile(xpath).load();
        }
    }

    private static class Helper {

        private static XdmNode getNodeFromFileObject(FileObject xmlFo) throws SaxonApiException, IOException {
            InputStream is = null;
            InputStream isSeq = null; // This stream is used for sequenceDiagram generation
            try {
                is = xmlFo.getContent().getInputStream();
                // Don't search sequences under soapUI-tests
                // "-soapui-project.xml" is uded as filename match pattern
                if (xmlFo.getName().getBaseName().indexOf("-soapui-project.xml") == -1) {
                    // Sequence diagram parser can't hande testa
                    isSeq = xmlFo.getContent().getInputStream();
                    String seg = SequenceDiagramBuilder.instance().buildPipe(isSeq);
                    isSeq.close();
                /*
                if (seg != null && !seg.isEmpty()) {
                    System.out.println("VALUE:" + seg);
                }
                */
                }
                XdmNode xmlNode = BUILDER.build(new StreamSource(is));
                XdmSequenceIterator i = xmlNode.axisIterator(Axis.CHILD);
                while (i.hasNext()) {
                    XdmItem item = i.next();
                    if (item instanceof XdmNode) {
                        xmlNode = (XdmNode) item;
                        if (xmlNode.getNodeKind() == XdmNodeKind.ELEMENT) {
                            return xmlNode;
                        }
                    }
                }
                throw new RuntimeException("Failed to find the root element");
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }


        private static <T> T convertXdmItemToType(XdmItem item, Class<T> type){
            if(String.class.equals(type)) {
                return (T) item.getStringValue();
            }
            //TODO support for booleans etc?
            return (T) item;
        }
    }
}
