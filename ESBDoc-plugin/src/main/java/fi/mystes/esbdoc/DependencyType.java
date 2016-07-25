package fi.mystes.esbdoc;

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;
import org.apache.commons.lang3.StringUtils;

import static fi.mystes.esbdoc.Constants.*;
import static fi.mystes.esbdoc.Constants.WSDL_RESOURCE_XPATH_STRING;
import static fi.mystes.esbdoc.Constants.WSDL_XPATH_STRING;

/**
 * Created by mystes-am on 21.5.2015.
 */
public enum DependencyType {

    SEQUENCE("sequence", SEQUENCE_XPATH_STRING),
    IN_SEQUENCE("inSequence", IN_SEQUENCE_XPATH_STRING),
    OUT_SEQUENCE("outSequence", OUT_SEQUENCE_XPATH_STRING),
    FAULT_SEQUENCE("faultSequence", FAULT_SEQUENCE_XPATH_STRING),
    ON_ERROR_SEQUENCE("onErrorSequence", ON_ERROR_SEQUENCE_XPATH_STRING),
    PROXY_ENDPOINT("proxyEndpoint", PROXY_ENDPOINT_XPATH_STRING, PROXY_ENDPOINT_ADDRESS_XPATH_STRING),
    CLONE("clone", CLONE_ENDPOINT_XPATH_STRING, CLONE_INLINE_ENDPOINT_XPATH_STRING, CLONE_SEQUENCE_XPATH_STRING),
    ITERATE("iterate", ITERATE_ENDPOINT_XPATH_STRING, ITERATE_INLINE_ENDPOINT_XPATH_STRING, ITERATE_SEQUENCE_XPATH_STRING),
    SEND("send", SEND_ENDPOINT_XPATH_STRING, SEND_ADDRESS_XPATH_STRING),
    CALL("call", CALL_ENDPOINT_XPATH_STRING, CALL_ADDRESS_XPATH_STRING),
    CALLOUT("callout", CALLOUT_XPATH_STRING, CUSTOM_CALLOUT_XPATH_STRING),
    ENDPOINT("endpoint", ENDPOINT_ADDRESS_XPATH_STRING),
    AINO_LOG("ainoLog", AINO_XPATH_STRING),
    REGISTRY("registry", SCRIPT_RESOURCE_XPATH_STRING, XSLT_RESOURCE_XPATH_STRING),
    STORE("store", STORE_XPATH_STRING),
    MESSAGE_PROCESSOR_STORE("messageStore", MESSAGE_PROCESSOR_STORE_XPATH_STRING),
    TASK_INJECT("inject", TASK_TARGET_XPATH_STRING),
    TASK_TO("taskTo", TASK_TO_XPATH_STRING),
    SMOOKS("smooks", SMOOKS_XPATH_STRING),
    DOCUMENTED("documented dependency"),
    SCHEMA("schema", SCHEMA_XPATH_STRING),
    WSDL("wsdl", WSDL_XPATH_STRING),
    WSDL_RESOURCE("wsdl resource", WSDL_RESOURCE_XPATH_STRING);

    private final String typeString;
    private final XPathSelector xPath;

    public String getTypeString() {
        return typeString;
    }

    public XPathSelector getXPath() {
        return xPath;
    }

    DependencyType(String typeString, String... xPaths) {
        String xPathUnion = StringUtils.join(xPaths, " | ");
        xPathUnion = StringUtils.trimToEmpty(xPathUnion);
        this.xPath = xPathForString(xPathUnion);
        this.typeString = typeString;
    }

    private XPathSelector xPathForString(String xpathString){
        if(StringUtils.isEmpty(xpathString)) { return null; }
        try {
            return SaxonXPath.forString(xpathString);
        } catch (SaxonApiException e) {
            throw new Error("Unable to initialize the DependencyType enum. Unable to compile XPath.", e);
        }
    }

    @Override
    public String toString() {
        return typeString;
    }
}
