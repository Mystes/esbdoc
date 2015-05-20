package fi.mystes.esbdoc;

import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.XPathCompiler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mystes-am on 20.5.2015.
 */
public class Constants {
    public static final String FILE_SEPARATOR = ",";

    public static final Processor PROCESSOR = new Processor(false);
    public static final DocumentBuilder BUILDER = PROCESSOR.newDocumentBuilder();
    public static final XPathCompiler COMPILER = PROCESSOR.newXPathCompiler();

    public static final QName ARTIFACT_Q = new QName("artifact");
    public static final QName VERSION_Q = new QName("version");
    public static final QName NAME_Q = new QName("name");
    public static final QName TYPE_Q = new QName("type");

    public static final javax.xml.namespace.QName PURPOSE_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "purpose");
    public static final javax.xml.namespace.QName RECEIVES_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "receives");
    public static final javax.xml.namespace.QName RETURNS_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "returns");
    public static final javax.xml.namespace.QName FIELD_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "field");
    public static final javax.xml.namespace.QName EXAMPLE_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "example");
    public static final javax.xml.namespace.QName DEPENDENCIES_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "dependencies");
    public static final javax.xml.namespace.QName DEPENDENCY_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "dependency");

    public static final javax.xml.namespace.QName DESCRIPTION_Q = new javax.xml.namespace.QName("description");
    public static final javax.xml.namespace.QName PATH_Q = new javax.xml.namespace.QName("path");
    public static final javax.xml.namespace.QName OPTIONAL_Q = new javax.xml.namespace.QName("optional");

    /* API related attributes */
    public static final javax.xml.namespace.QName CONTEXT_Q = new javax.xml.namespace.QName("context");
    public static final javax.xml.namespace.QName URL_MAPPING_Q = new javax.xml.namespace.QName("url-mapping");
    public static final javax.xml.namespace.QName METHODS_Q = new javax.xml.namespace.QName("methods");
    public static final javax.xml.namespace.QName RESOURCE_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "resource");

    public static final QName SOAPUI_PROJECT_Q = new QName("http://eviware.com/soapui/config", "soapui-project");
    public static final QName SOAPUI_TEST_SUITE_Q = new QName("http://eviware.com/soapui/config", "testSuite");
    public static final QName SOAPUI_TEST_CASE_Q = new QName("http://eviware.com/soapui/config", "testCase");
    public static final QName SOAPUI_TEST_STEP_Q = new QName("http://eviware.com/soapui/config", "testStep");
    public static final QName SOAPUI_ENDPOINT_Q = new QName("http://eviware.com/soapui/config", "endpoint");
    public static final QName SOAPUI_CONFIG_Q = new QName("http://eviware.com/soapui/config", "config");
    public static final QName SOAPUI_RESOURCE_PATH_Q = new QName("resourcePath");
    public static final QName SOAPUI_PROPERTIES_Q = new QName("http://eviware.com/soapui/config", "properties");
    public static final QName SOAPUI_PROPERTY_Q = new QName("http://eviware.com/soapui/config", "property");
    public static final QName SOAPUI_VALUE_Q = new QName("http://eviware.com/soapui/config", "value");
    public static final QName SOAPUI_NAME_Q = new QName("http://eviware.com/soapui/config", "name");

    public static final String[] IGNORED_ARTIFACT_TYPE_STRINGS = {
            "synapse/local-entry"
    };

    public static final Set<String> IGNORED_ARTIFACT_TYPES = new HashSet<String>();

    static {
        IGNORED_ARTIFACT_TYPES.addAll(Arrays.asList(IGNORED_ARTIFACT_TYPE_STRINGS));
        COMPILER.declareNamespace("s", "http://ws.apache.org/ns/synapse");
        COMPILER.declareNamespace("con", "http://eviware.com/soapui/config");
    }

    public static final String DEPENDENCY_XPATH_STRING = "/artifacts/artifact[@type = 'carbon/application']/dependency";
    public static final String ARTIFACT_FILENAME_XPATH_STRING = "/artifact/file/text()";
    public static final String TESTCASE_XPATH_STRING = "/con:soapui-project";

    public static final String ARTIFACT_DESCRIPTION_XPATH_STRING = "//s:description[s:purpose or s:receives or s:returns or s:dependencies]";

    // XPath strings to find interesting bits of artifacts
    public static final String SEQUENCE_XPATH_STRING = "//s:sequence/@key";
    public static final String IN_SEQUENCE_XPATH_STRING = "//s:target/@inSequence";
    public static final String OUT_SEQUENCE_XPATH_STRING = "//s:target/@outSequence";
    public static final String FAULT_SEQUENCE_XPATH_STRING = "//s:target/@faultSequence";
    public static final String ON_ERROR_SEQUENCE_XPATH_STRING = "/s:sequence/@onError";

    public static final String PROXY_ENDPOINT_XPATH_STRING = "/s:proxy/s:target/@endpoint";
    public static final String PROXY_ENDPOINT_ADDRESS_XPATH_STRING = "/s:proxy/s:target/s:endpoint/s:address/@uri";

    public static final String CLONE_SEQUENCE_XPATH_STRING = "//s:clone/s:target/@sequence";
    public static final String CLONE_ENDPOINT_XPATH_STRING = "//s:clone/s:target/@endpoint";
    public static final String CLONE_INLINE_ENDPOINT_XPATH_STRING = "//s:clone/s:target/s:endpoint/s:address/@uri";

    public static final String ITERATE_SEQUENCE_XPATH_STRING = "//s:iterate/s:target/@sequence";
    public static final String ITERATE_ENDPOINT_XPATH_STRING = "//s:iterate/s:target/@endpoint";
    public static final String ITERATE_INLINE_ENDPOINT_XPATH_STRING = "//s:iterate/s:target/s:endpoint/s:address/@uri";

    public static final String SEND_ENDPOINT_XPATH_STRING = "//s:send/s:endpoint/@key";
    public static final String SEND_ADDRESS_XPATH_STRING = "//s:send/s:endpoint/s:address/@uri";

    public static final String CALL_ENDPOINT_XPATH_STRING = "//s:call/s:endpoint/@key";
    public static final String CALL_ADDRESS_XPATH_STRING = "//s:call/s:endpoint/s:address/@uri";

    public static final String CALLOUT_XPATH_STRING = "//s:callout/@serviceURL";
    public static final String CUSTOM_CALLOUT_XPATH_STRING = "//s:customCallout/@serviceURL";

    public static final String ENDPOINT_ADDRESS_XPATH_STRING = "/s:endpoint/s:address/@uri";

    public static final String SCRIPT_RESOURCE_XPATH_STRING = "//s:script/@key";
    public static final String XSLT_RESOURCE_XPATH_STRING = "//s:xslt/@key";
    public static final String SMOOKS_XPATH_STRING = "//s:smooks/@config-key";
    public static final String SCHEMA_XPATH_STRING = "//s:schema/@key";
    public static final String WSDL_XPATH_STRING = "//s:publishWSDL/@key";
    public static final String WSDL_RESOURCE_XPATH_STRING = "//s:publishWSDL/s:resource/@key";

    // There is a potential point of discontinuity here since it can be quite non-trivial to determine the destination of a message stored in a message store
    public static final String STORE_XPATH_STRING = "//s:store/@messageStore";

    public static final String MESSAGE_PROCESSOR_STORE_XPATH_STRING = "/s:messageProcessor/@messageStore";

    public static final String TASK_TARGET_XPATH_STRING = "/s:task/s:property[@name = 'sequenceName']/@value | /s:task/s:property[@name = 'proxyName']/@value";

    public static final String TASK_TO_XPATH_STRING = "/s:task/s:property[@name = 'to']/@value";

    public static final String USAGE_HELP = "Usage: java -jar CarAnalyzer.jar [carFiles] [outputFile] [soapUIFiles]\n"
            + "  [carFiles]: comma-separated list of car file names\n"
            + "  [outputFile]: full name of the output file WITHOUT extension.\n"
            + "                Two files will be created, one with a .txt extension and another with a .json extension.\n"
            + "  [soapUIFolders]: comma-separated list of SoapUI folder names. (Optional argument)";
}
