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

import net.sf.saxon.s9api.QName;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by mystes-am on 20.5.2015.
 */
public class Constants {
    public static final String PHYSICAL_DEPENDENCY_JSON_FILE = "esbdoc-physical-dependencies.json";
    public static final String PHYSICAL_DEPENDENCY_TEXT_FILE = "esbdoc-physical-dependencies.txt";
    public static final String MSC_JSON_FILE = "esbdoc-message-sequence-chart.json";

    public static final Namespace SYNAPSE_NAMESPACE = new Namespace("s", "http://ws.apache.org/ns/synapse");
    public static final Namespace SOAPUI_CONFIG_NAMESPACE = new Namespace("con", "http://eviware.com/soapui/config");

    public static final QName ARTIFACT_Q = new QName("artifact");
    public static final QName VERSION_Q = new QName("version");
    public static final QName NAME_Q = new QName("name");
    public static final QName TYPE_Q = new QName("type");

    public static final javax.xml.namespace.QName PURPOSE_Q = new javax.xml.namespace.QName(SYNAPSE_NAMESPACE.URI, "purpose");
    public static final javax.xml.namespace.QName RECEIVES_Q = new javax.xml.namespace.QName(SYNAPSE_NAMESPACE.URI, "receives");
    public static final javax.xml.namespace.QName RETURNS_Q = new javax.xml.namespace.QName(SYNAPSE_NAMESPACE.URI, "returns");
    public static final javax.xml.namespace.QName FIELD_Q = new javax.xml.namespace.QName(SYNAPSE_NAMESPACE.URI, "field");
    public static final javax.xml.namespace.QName EXAMPLE_Q = new javax.xml.namespace.QName(SYNAPSE_NAMESPACE.URI, "example");
    public static final javax.xml.namespace.QName DEPENDENCIES_Q = new javax.xml.namespace.QName(SYNAPSE_NAMESPACE.URI, "dependencies");
    public static final javax.xml.namespace.QName DEPENDENCY_Q = new javax.xml.namespace.QName(SYNAPSE_NAMESPACE.URI, "dependency");
    public static final javax.xml.namespace.QName RESOURCE_Q = new javax.xml.namespace.QName(SYNAPSE_NAMESPACE.URI, "resource");

    public static final javax.xml.namespace.QName DESCRIPTION_Q = new javax.xml.namespace.QName("description");
    public static final javax.xml.namespace.QName PATH_Q = new javax.xml.namespace.QName("path");
    public static final javax.xml.namespace.QName OPTIONAL_Q = new javax.xml.namespace.QName("optional");

    /* API related attributes */
    public static final javax.xml.namespace.QName CONTEXT_Q = new javax.xml.namespace.QName("context");
    public static final javax.xml.namespace.QName URL_MAPPING_Q = new javax.xml.namespace.QName("url-mapping");
    public static final javax.xml.namespace.QName METHODS_Q = new javax.xml.namespace.QName("methods");

    public static final QName SOAPUI_PROJECT_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "soapui-project");
    public static final QName SOAPUI_TEST_SUITE_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "testSuite");
    public static final QName SOAPUI_TEST_CASE_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "testCase");
    public static final QName SOAPUI_TEST_STEP_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "testStep");
    public static final QName SOAPUI_ENDPOINT_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "endpoint");
    public static final QName SOAPUI_CONFIG_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "config");
    public static final QName SOAPUI_RESOURCE_PATH_Q = new QName("resourcePath");
    public static final QName SOAPUI_PROPERTIES_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "properties");
    public static final QName SOAPUI_PROPERTY_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "property");
    public static final QName SOAPUI_VALUE_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "value");
    public static final QName SOAPUI_NAME_Q = new QName(SOAPUI_CONFIG_NAMESPACE.URI, "name");

    public static final String[] IGNORED_ARTIFACT_TYPE_STRINGS = {
            "synapse/local-entry"
    };
    public static final Set<String> IGNORED_ARTIFACT_TYPES = new HashSet<String>(Arrays.asList(IGNORED_ARTIFACT_TYPE_STRINGS));

    public static final String FILE_SEPARATOR = ",";

    public static final String DEPENDENCY_XPATH_STRING = "/artifacts/artifact[@type = 'carbon/application']/dependency";
    public static final String TASK_XPATH_STRING = "/artifacts/artifact[@type = 'synapse/task']";
    public static final String ENDPOINT_XPATH_STRING = "/artifacts/artifact[@type = 'synapse/endpoint']";
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
    public static final String PROXY_ENDPOINT_FAILOVER_GROUP_ADDRESS_XPATH_STRING = "/s:endpoint/s:failover/s:endpoint/s:address/@uri";
    public static final String PROXY_ENDPOINT_FAILOVER_GROUP_HTTP_XPATH_STRING = "/s:endpoint/s:failover/s:endpoint/s:http/@uri-template";
    public static final String PROXY_ENDPOINT_HTTP_XPATH_STRING = "/s:endpoint/s:http/@uri-template";
    public static final String PROXY_ENDPOINT_LOADBALANCE_ADDRESS_XPATH_STRING = "/s:endpoint/s:loadbalance/s:endpoint/s:address/@uri";
    public static final String PROXY_ENDPOINT_LOADBALANCE_HTTP_XPATH_STRING = "/s:endpoint/s:loadbalance/s:endpoint/s:http/@uri-template";
    public static final String PROXY_ENDPOINT_RECIPIENT_LIST_ADDRESS_XPATH_STRING = "/s:endpoint/s:recipientlist/s:endpoint/s:address/@uri";
    public static final String PROXY_ENDPOINT_RECIPIENT_LIST_HTTP_XPATH_STRING = "/s:endpoint/s:recipientlist/s:endpoint/s:http/@uri-template";

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

    public static final String AINO_XPATH_STRING = "//s:ainoLog";

}
