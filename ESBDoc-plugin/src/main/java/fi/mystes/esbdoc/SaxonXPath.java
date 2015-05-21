package fi.mystes.esbdoc;

import net.sf.saxon.s9api.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
    private static Log log = LogFactory.getLog(SaxonXPath.class);

    private static final Processor processor = new Processor(false);
    private static final XPathCompiler compiler = processor.newXPathCompiler();

    static {
        compiler.declareNamespace(Constants.SYNAPSE_NAMESPACE.PREFIX, Constants.SYNAPSE_NAMESPACE.URI);
        compiler.declareNamespace(Constants.SOAPUI_CONFIG_NAMESPACE.PREFIX, Constants.SOAPUI_CONFIG_NAMESPACE.URI);
    }

    private SaxonXPath(){};

    public static Builder apply(String thisXpath){
        return new Builder().using(thisXpath);
    }

    public static Builder apply(XPathSelector thisXpath){
        return new Builder().using(thisXpath);
    }

    public static XPathSelector forString(String thisXpath) throws SaxonApiException {
        return new Executor().load(thisXpath);
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

        private Executor(){};

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
            return compiler.compile(xpath);
        }

        private XPathSelector load(String xpath) throws SaxonApiException {
            return compile(xpath).load();
        }
    }

    protected static class Helper {
        public static final DocumentBuilder BUILDER = processor.newDocumentBuilder();

        private static XdmNode getNodeFromFileObject(FileObject xmlFileObject) throws SaxonApiException, IOException {
            //TODO This logic should be relocated
            if (!isFileObjectASoapUiProject(xmlFileObject)) {
                log.debug("FileObject is not a SoapUI project and therefore it can be used to construct Sequence Diagrams.");
                buildSequenceDiagrams(xmlFileObject);
            }

            log.debug("Starting to look for the root element of given file object...");
            return findRootElement(xmlFileObject);
        }

        private static XdmNode findRootElement(FileObject xmlFileObject) throws SaxonApiException, IOException {
            InputStream inputStream = xmlFileObject.getContent().getInputStream();
            if (null == inputStream) {
                log.debug("getNodeFromFileObject: InputStream is null, nothing to do!");
                return null;
            }
            try {
                return findRootElement(inputStream);
            } finally {
                log.debug("getNodeFromFileObject: Closing InputStream...");
                inputStream.close();
            }
        }

        private static XdmNode findRootElement(InputStream inputStream) throws SaxonApiException {
            XdmNode xdmNode = BUILDER.build(new StreamSource(inputStream));
            XdmSequenceIterator i = xdmNode.axisIterator(Axis.CHILD);
            return findRootElement(xdmNode);
        }

        private static XdmNode findRootElement(XdmNode xdmNode){
            XdmSequenceIterator i = xdmNode.axisIterator(Axis.CHILD);
            while (i.hasNext()) {
                XdmItem item = i.next();
                if (isRootElement(item)) {
                    return (XdmNode) item;
                }
            }
            //TODO Is RuntimeException really The Thing to throw - or should we have a custom exception?
            throw new RuntimeException("Failed to find the root element");
        }

        private static boolean isRootElement(XdmItem xdmItem){
            return isXdmNode(xdmItem) && isElement(xdmItem);
        }

        private static boolean isXdmNode(XdmItem xdmItem){
            return xdmItem instanceof XdmNode;
        }

        private static boolean isElement(XdmItem xdmItem){
            return isElement((XdmNode) xdmItem);
        }

        private static boolean isElement(XdmNode xdmNode){
            return xdmNode.getNodeKind() == XdmNodeKind.ELEMENT;
        }

        private static boolean isFileObjectASoapUiProject(FileObject fileObject){
            String fileName = fileObject.getName().getBaseName();
            return StringUtils.contains(fileName, "-soapui-project.xml");
        }

        /**
         * FIXME This has nothing to do with SaxonXPath processing and needs to be relocated elsewhere!
         */
        private static void buildSequenceDiagrams(FileObject fileObject) {
            try {
                log.trace("buildSequenceDiagrams: Opening InputStream...");
                InputStream inputStreamForSequenceDiagrams = fileObject.getContent().getInputStream();
                String seg = SequenceDiagramBuilder.instance().buildPipe(inputStreamForSequenceDiagrams);
                log.trace("buildSequenceDiagrams: Closing InputStream...");
                inputStreamForSequenceDiagrams.close();
            } catch (IOException ioe){
                log.error("buildSequenceDiagrams: Could not close InputStream! Reason: " + ioe.getMessage());
            }
        }


        private static <T> T convertXdmItemToType(XdmItem item, Class<T> type){
            log.trace("convertXdmItemToType: Received request to convert XdmItem into " + type.getName());
            if(String.class.equals(type)) {
                return (T) item.getStringValue();
            }
            //TODO support for booleans etc?
            log.warn("convertXdmItemToType: Unsupported conversion request. I'll give it my best shot but this is probably doomed to fail.");
            return (T) item;
        }
    }
}
