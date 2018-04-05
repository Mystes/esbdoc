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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mystes-am on 20.5.2015.
 */
public class SaxonXPath {
    private static Log log = LogFactory.getLog(SaxonXPath.class);

    private static final XPathCompiler compiler = SaxonUtil.getProcessor().newXPathCompiler();

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
            this.executor.node = SaxonUtil.getNodeFromFileObject(xmlFo);
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

    private static class Helper {

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
