/**
 * Copyright 2018 Mystes Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.mystes.esbdoc;

import net.sf.saxon.s9api.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;

import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by mystes-am on 21.5.2015.
 */
public class SaxonUtil {
    private static Log log = LogFactory.getLog(SaxonUtil.class);

    private static final Processor processor = new Processor(false);
    private static final DocumentBuilder builder = processor.newDocumentBuilder();

    private SaxonUtil(){}

    public static XdmNode readToNode(InputStream inputStream) throws SaxonApiException {
        //TODO null handler?
        return builder.build(new StreamSource(inputStream));
    }

    //FIXME This method does more than one thing and that's against the laws of man and God too.
    public static XdmNode getNodeFromFileObject(FileObject xmlFileObject) throws SaxonApiException, IOException {
        //TODO null handler?
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
        XdmNode xdmNode = builder.build(new StreamSource(inputStream));
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
     * TODO This isn't exactly Saxon-related and hence should probably be relocated
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

    public static Processor getProcessor(){
        return processor;
    }
}
