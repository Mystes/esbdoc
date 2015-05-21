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

    public static final Processor PROCESSOR = new Processor(false);
    public static final DocumentBuilder BUILDER = PROCESSOR.newDocumentBuilder();
    public static final XPathCompiler COMPILER = PROCESSOR.newXPathCompiler();

    static {
        COMPILER.declareNamespace(Constants.SYNAPSE_NAMESPACE.PREFIX, Constants.SYNAPSE_NAMESPACE.URI);
        COMPILER.declareNamespace(Constants.SOAPUI_CONFIG_NAMESPACE.PREFIX, Constants.SOAPUI_CONFIG_NAMESPACE.URI);
    }

    private SaxonXPath(){};

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
            return COMPILER.compile(xpath);
        }

        private XPathSelector load(String xpath) throws SaxonApiException {
            return compile(xpath).load();
        }
    }

    private static class Helper {

        private static XdmNode getNodeFromFileObject(FileObject xmlFileObject) throws SaxonApiException, IOException {
            InputStream inputStream = null;
            try {
                inputStream = xmlFileObject.getContent().getInputStream();
                if (!isFileObjectASoapUiProject(xmlFileObject)) {
                    buildSequenceDiagrams(xmlFileObject);
                }
                XdmNode xmlNode = BUILDER.build(new StreamSource(inputStream));
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
                if (null == inputStream) {
                    log.warn("getNodeFromFileObject: Cannot close InputStream because it is null!");
                } else {
                    log.debug("getNodeFromFileObject: Closing InputStream...");
                    inputStream.close();
                }
            }
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
                InputStream inputStreamForSequenceDiagrams = fileObject.getContent().getInputStream();
                String seg = SequenceDiagramBuilder.instance().buildPipe(inputStreamForSequenceDiagrams);
                inputStreamForSequenceDiagrams.close();
            } catch (IOException ioe){
                log.error("Could not close InputStream for SequenceDiagrams! Reason: " + ioe.getMessage());
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
