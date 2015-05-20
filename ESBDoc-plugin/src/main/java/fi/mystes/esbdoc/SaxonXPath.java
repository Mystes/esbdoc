package fi.mystes.esbdoc;

import net.sf.saxon.s9api.*;
import org.apache.commons.vfs2.FileObject;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

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

    private static XPathExecutable compile(String xpath) throws SaxonApiException{
        return COMPILER.compile(xpath);
    }

    private static XPathSelector load(String xpath) throws SaxonApiException{
        return compile(xpath).load();
    }

    public static Builder apply(String thisXpath){
        return new Builder().using(thisXpath);
    }

    public static class Builder {
        private String xpathString;
        private XdmNode node;

        public Builder using(String thisXpath){
            this.xpathString = thisXpath;
            return this;
        }

        public Builder to(FileObject xmlFo) throws SaxonApiException, IOException {
            this.node = getNodeFromFileObject(xmlFo);
            return this;
        }

        public Builder to(XdmNode thisNode) {
            this.node = thisNode;
            return this;
        }

        public XdmItem andGiveMeASingleItem() throws SaxonApiException {
            XPathSelector selector = load(this.xpathString);
            selector.setContextItem(node);
            return selector.evaluateSingle();
        }

        public XdmValue andGiveMeAListOfItems() throws SaxonApiException {
            XPathSelector selector = load(this.xpathString);
            selector.setContextItem(node);
            return selector.evaluate();
        }

        public String please() throws SaxonApiException {
            return andGiveMeASingleItem().toString();
        }

        private XdmNode getNodeFromFileObject(FileObject xmlFo) throws SaxonApiException, IOException {
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
    }
}
