package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathSelector;

import static fi.mystes.esbdoc.Constants.*;

/**
 * Represents the different dependency types between artifacts
 */
public class Dependency implements Comparable<Dependency> {

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

        final String typeString;
        final XPathSelector xPath;

        public String getTypeString() {
            return typeString;
        }

        public XPathSelector getXPath() {
            return xPath;
        }

        DependencyType(String typeString, String... xPaths) {
            this.typeString = typeString;

            String xPathUnion = "";
            boolean first = true;
            for (String xPath : xPaths) {
                if (!first) {
                    xPathUnion += " | ";
                }
                xPathUnion += xPath;
                first = false;
            }

            if (!xPathUnion.trim().isEmpty()) {
                try {
                    this.xPath = SaxonXPath.COMPILER.compile(xPathUnion).load();
                } catch (SaxonApiException e) {
                    throw new Error("Unable to initialize the DependencyType enum. Unable to compile XPath.", e);
                }
            } else {
                this.xPath = null;
            }
        }

        @Override
        public String toString() {
            return typeString;
        }
    }

    public final Artifact dependent;
    public final Object dependency;
    private final DependencyType type;

    public Dependency(Artifact dependent, Object dependency, DependencyType dependencyType) {
        if (dependent == null || dependency == null || dependencyType == null) {
            throw new IllegalArgumentException("All Dependency constructor parameters must be non-null");
        }
        this.dependent = dependent;
        this.dependency = dependency;
        this.type = dependencyType;
    }

    public Object getDependency() {
        return dependency;
    }

    public DependencyType getType() {
        return type;
    }

    @Override
    public int compareTo(Dependency dependency) {
        if (this.dependency instanceof Artifact && dependency.dependency instanceof Artifact
                || this.dependency instanceof String && dependency.dependency instanceof String) {
            return ((Comparable) this.dependency).compareTo((Comparable) dependency.dependency);
        } else {
            if (this.dependency instanceof Artifact) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o != null && o instanceof Dependency) {
            Dependency other = (Dependency) o;
            return dependent.equals(other.dependent) && dependency.equals(other.dependency) && type.equals(other.type);
        }

        return false;
    }

    @Override
    public int hashCode() {
        int result = 37;
        result = 37 * result + dependent.hashCode();
        result = 37 * result + dependency.hashCode();
        result = 37 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        String dependencyString = dependency instanceof Artifact ? ((Artifact) dependency).getName() : (String) dependency;
        return dependent.getName() + " -> " + dependencyString + " :[" + type + "]";
    }
}
