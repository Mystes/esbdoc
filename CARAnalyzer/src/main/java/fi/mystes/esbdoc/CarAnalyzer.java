package fi.mystes.esbdoc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XPathCompiler;
import net.sf.saxon.s9api.XPathSelector;
import net.sf.saxon.s9api.XdmItem;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.s9api.XdmNodeKind;
import net.sf.saxon.s9api.XdmSequenceIterator;
import net.sf.saxon.s9api.XdmValue;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.jaxen.JaxenException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;

/**
 * Created by Jarkko on 1.10.2014.
 */
public class CarAnalyzer {

    private static Log log = LogFactory.getLog(CarAnalyzer.class);

    private static final Processor PROCESSOR = new Processor(false);
    private static final DocumentBuilder BUILDER = PROCESSOR.newDocumentBuilder();
    private static final XPathCompiler COMPILER = PROCESSOR.newXPathCompiler();

    private static final QName ARTIFACT_Q = new QName("artifact");
    private static final QName VERSION_Q = new QName("version");
    private static final QName NAME_Q = new QName("name");
    private static final QName TYPE_Q = new QName("type");

    private static final javax.xml.namespace.QName PURPOSE_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "purpose");
    private static final javax.xml.namespace.QName RECEIVES_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "receives");
    private static final javax.xml.namespace.QName RETURNS_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "returns");
    private static final javax.xml.namespace.QName FIELD_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "field");
    private static final javax.xml.namespace.QName EXAMPLE_Q = new javax.xml.namespace.QName("http://ws.apache.org/ns/synapse", "example");

    private static final javax.xml.namespace.QName DESCRIPTION_Q = new javax.xml.namespace.QName("description");
    private static final javax.xml.namespace.QName PATH_Q = new javax.xml.namespace.QName("path");
    private static final javax.xml.namespace.QName OPTIONAL_Q = new javax.xml.namespace.QName("optional");

    private static final String[] IGNORED_ARTIFACT_TYPE_STRINGS = {
            "registry/resource",
            "synapse/local-entry"
    };

    private static final Set<String> IGNORED_ARTIFACT_TYPES = new HashSet<String>();

    static {
        IGNORED_ARTIFACT_TYPES.addAll(Arrays.asList(IGNORED_ARTIFACT_TYPE_STRINGS));
        COMPILER.declareNamespace("s", "http://ws.apache.org/ns/synapse");
    }

    private static final String DEPENDENCY_XPATH_STRING = "/artifacts/artifact[@type = 'carbon/application']/dependency";
    private static final String ARTIFACT_FILENAME_XPATH_STRING = "/artifact/file/text()";

    private static final String ARTIFACT_DESCRIPTION_XPATH_STRING = "//s:description[s:purpose or s:receives or s:returns]";

    // XPath strings to find interesting bits of artifacts
    private static final String SEQUENCE_XPATH_STRING = "//s:sequence/@key";
    private static final String IN_SEQUENCE_XPATH_STRING = "//s:target/@inSequence";
    private static final String OUT_SEQUENCE_XPATH_STRING = "//s:target/@outSequence";
    private static final String FAULT_SEQUENCE_XPATH_STRING = "//s:target/@faultSequence";
    private static final String ON_ERROR_SEQUENCE_XPATH_STRING = "/s:sequence/@onError";

    private static final String PROXY_ENDPOINT_XPATH_STRING = "/s:proxy/s:target/@endpoint";
    private static final String PROXY_ENDPOINT_ADDRESS_XPATH_STRING = "/s:proxy/s:target/s:endpoint/s:address/@uri";

    private static final String CLONE_SEQUENCE_XPATH_STRING = "//s:clone/s:target/@sequence";
    private static final String CLONE_ENDPOINT_XPATH_STRING = "//s:clone/s:target/@endpoint";
    private static final String CLONE_INLINE_ENDPOINT_XPATH_STRING = "//s:clone/s:target/s:endpoint/s:address/@uri";

    private static final String ITERATE_SEQUENCE_XPATH_STRING = "//s:iterate/s:target/@sequence";
    private static final String ITERATE_ENDPOINT_XPATH_STRING = "//s:iterate/s:target/@endpoint";
    private static final String ITERATE_INLINE_ENDPOINT_XPATH_STRING = "//s:iterate/s:target/s:endpoint/s:address/@uri";

    private static final String SEND_ENDPOINT_XPATH_STRING = "//s:send/s:endpoint/@key";
    private static final String SEND_ADDRESS_XPATH_STRING = "//s:send/s:endpoint/s:address/@uri";

    private static final String CALL_ENDPOINT_XPATH_STRING = "//s:call/s:endpoint/@key";
    private static final String CALL_ADDRESS_XPATH_STRING = "//s:call/s:endpoint/s:address/@uri";

    private static final String CALLOUT_XPATH_STRING = "//s:callout/@serviceURL";
    private static final String CUSTOM_CALLOUT_XPATH_STRING = "//s:customCallout/@serviceURL";

    private static final String ENDPOINT_ADDRESS_XPATH_STRING = "/s:endpoint/s:address/@uri";

    // There is a potential point of discontinuity here since it can be quite non-trivial to determine the destination of a message stored in a message store
    private static final String STORE_XPATH_STRING = "//s:store/@messageStore";

    private static final String MESSAGE_PROCESSOR_STORE_XPATH_STRING = "/s:messageProcessor/@messageStore";

    private static final String TASK_TARGET_XPATH_STRING = "/s:task/s:property[@name = 'sequenceName']/@value | /s:task/s:property[@name = 'proxyName']/@value";

    private static final String TASK_TO_XPATH_STRING = "/s:task/s:property[@name = 'to']/@value";

    private static final String USAGE_HELP = "Usage: java -jar CarAnalyzer.jar [carFiles] [outputFile]\n" +
            "  [carFiles]: semicolon-separated list of car file names\n" +
            "  [outputFile]: full name of the output file WITHOUT extension.\n" +
            "                Two files will be created, one with a .txt extension and another with a .json extension.";

    private AXIOMXPath xpath;

    private final XPathSelector dependencyXPath;
    private final XPathSelector artifactFilenameXPath;
    private final XPathSelector artifactDescriptionXPath;

    private SortedMap<String, Artifact> artifactMap = new TreeMap<String, Artifact>();

    private SortedMap<Artifact, Set<Dependency>> forwardDependencyMap = new TreeMap<Artifact, Set<Dependency>>();

    private SortedMap<Artifact, Set<Dependency>> reverseDependencyMap = new TreeMap<Artifact, Set<Dependency>>();

    private FileSystemManager fsm;

    public CarAnalyzer() throws FileSystemException, ParserConfigurationException, JaxenException {
        fsm = VFS.getManager();
        try {
            dependencyXPath = COMPILER.compile(DEPENDENCY_XPATH_STRING).load();
            artifactFilenameXPath = COMPILER.compile(ARTIFACT_FILENAME_XPATH_STRING).load();
            artifactDescriptionXPath = COMPILER.compile(ARTIFACT_DESCRIPTION_XPATH_STRING).load();
        } catch (SaxonApiException e) {
            throw new RuntimeException("Unable to initialize the CarCallTree class", e);
        }

        xpath = new AXIOMXPath(ARTIFACT_DESCRIPTION_XPATH_STRING);
        xpath.addNamespace("s", "http://ws.apache.org/ns/synapse");
    }

    public static void main(String[] args) throws IOException, SaxonApiException, ParserConfigurationException, SAXException, XPathExpressionException, JaxenException {
        log.info("Running...");

        CarAnalyzer cct = new CarAnalyzer();

        if (!checkInput(args)) {
            return;
        }

        String outputFilename = args[1];

        List<FileObject> carFileObjects = cct.getCarFileObjects(args[0]);

        cct.getArtifactMap(carFileObjects);

        Map<Artifact, Set<Dependency>> dependencyMap = cct.getForwardDependencyMap();

        new File(outputFilename).getParentFile().mkdirs();

        FileOutputStream fis = null;
        fis = new FileOutputStream(new File(outputFilename + ".txt"));
        cct.writeText(fis);
        fis.close();

        fis = new FileOutputStream(new File(outputFilename + ".json"));
        cct.writeJson(fis);
        fis.close();

        // The dependencies (including transitive ones) for a specific proxy are printed to the console as a test.
        //for (Dependency d : cct.getDependencyList(cct.artifactMap.get("Billing_processFixedLengthZLaskuProxy"))) {
        //    System.out.println(d);
        //}

        log.info("Done!");
    }

    private static boolean checkInput(String[] args) {
        if (args.length < 2 || args[0] == null || args[0].isEmpty() || args[1] == null || args[1].isEmpty()) {
            System.out.println(USAGE_HELP);
            return false;
        }
        return true;
    }


    /**
     * Retrieves an ordered list of Dependencies depicting the dependency tree of the given artifact in a depth-first fashion
     *
     * @param a
     * @return
     */
    private List<Dependency> getDependencyList(Artifact a) {
        List<Dependency> dependencyList = new ArrayList<Dependency>();
        buildDependencyList(a, dependencyList, new HashSet<Artifact>());
        return dependencyList;
    }

    private void writeText(OutputStream os) throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(os, Charset.forName("UTF-8"));
        for (Map.Entry<Artifact, Set<Dependency>> entry : forwardDependencyMap.entrySet()) {
            for (Dependency d : entry.getValue()) {
                osw.write(d.toString());
                osw.write('\n');
            }
        }
        osw.close();
    }

    private void writeJson(OutputStream os) throws IOException {
        JsonFactory factory = new JsonFactory();
        JsonGenerator generator = factory.createGenerator(os);
        generator.writeStartObject();

        generator.writeObjectFieldStart("resources");
        for (Map.Entry<String, Artifact> entry : artifactMap.entrySet()) {
            Artifact a = entry.getValue();
            generator.writeObjectFieldStart(a.getName());
            if (a.description != null) {
                writeArtifactDescriptionJson(a.description, generator);
            }
            generator.writeStringField("type", a.getType().toString());
            generator.writeEndObject();
        }
        generator.writeEndObject();

        generator.writeObjectFieldStart("dependencies");

        generator.writeObjectFieldStart("forward");
        for (Map.Entry<Artifact, Set<Dependency>> entry : forwardDependencyMap.entrySet()) {
            generator.writeArrayFieldStart(entry.getKey().getName());

            for (Dependency d : entry.getValue()) {
                // Currently only Artifacts are included in the JSON output
                if (d.getDependency() instanceof Artifact) {
                    generator.writeStartObject();
                    generator.writeStringField("target", ((Artifact)d.getDependency()).getName());
                    generator.writeStringField("type", d.getType().toString());
                    generator.writeEndObject();
                }
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();

        generator.writeObjectFieldStart("reverse");
        for (Map.Entry<Artifact, Set<Dependency>> entry : reverseDependencyMap.entrySet()) {
            generator.writeArrayFieldStart(entry.getKey().getName());
            for (Dependency d : entry.getValue()) {
                generator.writeStartObject();
                generator.writeStringField("source", d.dependent.getName());
                generator.writeStringField("type", d.getType().toString());
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }
        generator.writeEndObject();

        generator.writeEndObject();
        generator.writeEndObject();
        generator.close();
    }

    private void writeArtifactDescriptionJson(Artifact.ArtifactDescription ad, JsonGenerator generator) throws IOException {
        if (ad.purpose != null)
            generator.writeStringField("purpose", ad.purpose);

        if (ad.receives != null) {
            generator.writeObjectFieldStart("receives");
            writeArtifactInterfaceInfoJson(ad.receives, generator);
            generator.writeEndObject();
        }

        if (ad.returns != null) {
            generator.writeObjectFieldStart("returns");
            writeArtifactInterfaceInfoJson(ad.returns, generator);
            generator.writeEndObject();
        }
    }

    private void writeArtifactInterfaceInfoJson(Artifact.ArtifactInterfaceInfo aii, JsonGenerator generator) throws IOException {
        if (aii.description != null) {
            generator.writeStringField("description", aii.description);
        }

        if (aii.fields != null) {
            generator.writeArrayFieldStart("fields");
            for (Artifact.ArtifactIntefaceField f : aii.fields) {
                generator.writeStartObject();
                generator.writeStringField("description", f.description);
                generator.writeStringField("path", f.path);
                generator.writeBooleanField("optional", f.optional);
                generator.writeEndObject();
            }
            generator.writeEndArray();
        }

        if (aii.example != null) {
            generator.writeStringField("example", aii.example);
        }
    }



    private void buildDependencyList(Artifact a, List<Dependency> dependencyList, Set<Artifact> visitedNodes) {
        visitedNodes.add(a);

        Set<Dependency> dependencies = forwardDependencyMap.get(a);
        if (dependencies == null) {
            return;
        }

        for (Dependency d : dependencies) {
            Object dependencyObject = d.getDependency();
            dependencyList.add(d);
            if (dependencyObject instanceof Artifact && !visitedNodes.contains((Artifact)dependencyObject)) {
                buildDependencyList((Artifact)dependencyObject, dependencyList, visitedNodes);
            }
        }
    }

    /**
     * Returns a list of Apache VFS FileObjects pointing to the parameter car files
     * @param carNames a semicolon separated list of car file paths
     * @return
     * @throws FileSystemException
     */
    private List<FileObject> getCarFileObjects(String carNames) throws FileSystemException {
        String[] carNameArray = carNames.split(";");
        List<FileObject> carFileObjects = new ArrayList<FileObject>(carNameArray.length);

        for (String carName : carNameArray) {
            File f = new File(carName);
            if (f.exists()) {
                carFileObjects.add(fsm.resolveFile("zip:" + f.getAbsolutePath()));
            } else {
                log.warn(MessageFormat.format("The specified car file [{0}] does not exist.", carName));
            }
        }

        return carFileObjects;
    }

    /**
     * Returns a map mapping artifact names to the artifacts in the car files pointed to by the given FileObjects
     * @param carFileObjects
     * @return
     * @throws IOException
     * @throws SaxonApiException
     */
    private SortedMap<String, Artifact> getArtifactMap(List<FileObject> carFileObjects) throws IOException, SaxonApiException, SAXException, XPathExpressionException, JaxenException {
        for (FileObject carFileObject : carFileObjects) {
            System.out.println("carFileObject: " + carFileObject.getName());
            FileObject artifactsFileObject = carFileObject.getChild("artifacts.xml");
            log.info(MessageFormat.format("Processing artifacts.xml file: [{0}]", artifactsFileObject.getURL().toString()));
            XdmNode artifactsNode = getNodeFromFileObject(artifactsFileObject);
            dependencyXPath.setContextItem(artifactsNode);
            XdmValue value = dependencyXPath.evaluate();
            for (XdmItem item : value) {
                Artifact a = getArtifact((XdmNode) item, carFileObject);
                if (a != null) {
                    artifactMap.put(a.getName(), a);
                }
            }
        }
        return artifactMap;
    }

    /**
     * Returns an XdmNode pointing to the root element of the XML in the specified file
     * @param xmlFo
     * @return
     * @throws SaxonApiException
     * @throws IOException
     */
    private XdmNode getNodeFromFileObject(FileObject xmlFo) throws SaxonApiException, IOException {
        InputStream is = null;
        try {
            is = xmlFo.getContent().getInputStream();
            XdmNode xmlNode = BUILDER.build(new StreamSource(is));
            XdmSequenceIterator i = xmlNode.axisIterator(Axis.CHILD);
            while (i.hasNext()) {
                XdmItem item = i.next();
                if (item instanceof XdmNode) {
                    xmlNode = (XdmNode)item;
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

    /**
     * Returns an Artifact object for the given XdmNode containing an artifact definition from an artifact.xml file
     * @param dependencyNode
     * @param carFile
     * @return
     * @throws IOException
     * @throws SaxonApiException
     */
    private Artifact getArtifact(XdmNode dependencyNode, FileObject carFile) throws IOException, SaxonApiException, SAXException, XPathExpressionException, JaxenException {
        String dependencyName = dependencyNode.getAttributeValue(ARTIFACT_Q);
        String dependencyVersion = dependencyNode.getAttributeValue(VERSION_Q);

        StringBuilder dependencyArtifactFilePathBuilder = new StringBuilder()
                .append(dependencyName).append('_').append(dependencyVersion)
                .append('/');

        String dependencyDirectory = dependencyArtifactFilePathBuilder.toString();

        dependencyArtifactFilePathBuilder.append("artifact.xml");

        FileObject artifactFileObject = carFile.resolveFile(dependencyArtifactFilePathBuilder.toString());

        if (artifactFileObject == null || !artifactFileObject.exists()) {
            return null;
        }

        XdmNode artifactFileXml = getNodeFromFileObject(artifactFileObject);

        artifactFilenameXPath.setContextItem(artifactFileXml);

        String artifactFilePath = artifactFilenameXPath.evaluateSingle().toString();

        String artifactName = getRealNameForArtifact(carFile.toString() + dependencyDirectory + artifactFilePath);

        Artifact.ArtifactDescription description = getArtifactDescription(carFile.toString() + dependencyDirectory + artifactFilePath);

        String artifactTypeString = artifactFileXml.getAttributeValue(TYPE_Q);

        Artifact.ArtifactType artifactType = Artifact.ArtifactType.getArtifactTypeByTypeString(artifactTypeString);

        if (artifactType == null && !IGNORED_ARTIFACT_TYPES.contains(artifactTypeString)) {
            log.warn("Unrecognized artifact type: " + artifactTypeString);
            return null;
        } else if (IGNORED_ARTIFACT_TYPES.contains(artifactTypeString)) {
            return null;
        }

        return new Artifact(artifactName, dependencyVersion, artifactType, dependencyDirectory + artifactFilePath, carFile.toString(), description);
    }

    private Artifact.ArtifactDescription getArtifactDescription(String artifactFilePath) throws IOException, JaxenException {
        FileObject artifactFileObject = fsm.resolveFile(artifactFilePath);

        OMElement root = OMXMLBuilderFactory.createOMBuilder(artifactFileObject.getContent().getInputStream()).getDocumentElement();

        Object evaluationResult = xpath.evaluate(root);
        if (evaluationResult instanceof List) {
            List resultList = (List)evaluationResult;

            if (!resultList.isEmpty()) {
                OMElement descriptionElement = (OMElement)resultList.get(0);
                if (descriptionElement != null) {
                    String purpose = null;
                    Artifact.ArtifactInterfaceInfo receives = null, returns = null;

                    OMElement purposeElement = descriptionElement.getFirstChildWithName(PURPOSE_Q);
                    if (purposeElement != null) {
                        purpose = purposeElement.getText();
                    }

                    OMElement receivesElement = descriptionElement.getFirstChildWithName(RECEIVES_Q);
                    if (receivesElement != null) {
                        receives = getArtifactInterfaceInfo(receivesElement);
                    }

                    OMElement returnsElement = descriptionElement.getFirstChildWithName(RETURNS_Q);
                    if (returnsElement != null) {
                        returns = getArtifactInterfaceInfo(returnsElement);
                    }

                    if (purpose != null || receives != null || returns != null) {
                        return new Artifact.ArtifactDescription(purpose, receives, returns);
                    }
                }
            }
        }

        return null;
    }

    private Artifact.ArtifactInterfaceInfo getArtifactInterfaceInfo(OMElement infoElement) {
        Artifact.ArtifactInterfaceInfo aii = new Artifact.ArtifactInterfaceInfo();
        String description = infoElement.getText();
        if (description != null) {
            description = description.trim();

            if (!description.isEmpty()) {
                aii.description = description;
            }
        }

        OMElement exampleElement = infoElement.getFirstChildWithName(EXAMPLE_Q);
        if (exampleElement != null) {
            exampleElement = exampleElement.getFirstElement();

            if (exampleElement != null) {
                String example = exampleElement.toString();
                if (example != null) {
                    example = example.trim();

                    if (!example.isEmpty()) {
                        aii.example = example;
                    }
                }
            }
        }

        Iterator<OMElement> fields = infoElement.getChildrenWithName(FIELD_Q);
        while (fields.hasNext()) {
            OMElement field = fields.next();

            String fieldDescription = field.getAttributeValue(DESCRIPTION_Q);
            String fieldPath = field.getAttributeValue(PATH_Q);
            boolean isOptional = "true".equals(field.getAttributeValue(OPTIONAL_Q));

            if (fieldPath != null) {
                aii.addField(new Artifact.ArtifactIntefaceField(fieldDescription, fieldPath, isOptional));
            }
        }

        if (aii.description != null || aii.fields != null) {
            return aii;
        }

        return null;
    }

    /**
     * Returns the name of the artifact defined in the given file
     * @param artifactFilePath
     * @return
     * @throws IOException
     * @throws SaxonApiException
     */
    private String getRealNameForArtifact(String artifactFilePath) throws IOException, SaxonApiException {
        FileObject artifactFileObject = fsm.resolveFile(artifactFilePath);
        XdmNode artifactXml = getNodeFromFileObject(artifactFileObject);
        return artifactXml.getAttributeValue(NAME_Q);
    }

    /**
     * Returns a map mapping artifacts to their direct dependencies
     * @return
     * @throws SaxonApiException
     * @throws IOException
     */
    private SortedMap<Artifact, Set<Dependency>> getForwardDependencyMap() throws SaxonApiException, IOException {
        for (Artifact a : artifactMap.values()) {
            FileObject artifactFileObject = fsm.resolveFile(a.getCarPath() + a.getPath());
            if (artifactFileObject.exists()) {
                XdmNode artifactXml = getNodeFromFileObject(artifactFileObject);

                Set<Dependency> dependencies = new HashSet<Dependency>();

                for (Dependency.DependencyType dt : Dependency.DependencyType.values()) {
                    // TASK_TO has special handling
                    if (dt == Dependency.DependencyType.TASK_TO)
                        continue;

                    dependencies.addAll(getDepdendencySet(a, artifactXml, dt));
                }

                if (a.getType() == Artifact.ArtifactType.TASK && !dependencies.isEmpty()) {
                    // A task should only have a single dependency, a proxy or a sequence
                    if (dependencies.size() > 1) {
                        System.out.println("The task: " + a.getName() + " has multiple dependencies. This is probably an error.");
                    }

                    Dependency.DependencyType dt = Dependency.DependencyType.TASK_TO;
                    Set<String> dependencyString = evaluateXPathToStringSet(artifactXml, dt.getXPath());

                    if (dependencyString != null && !dependencyString.isEmpty()) {
                        if (dependencyString.size() > 1) {
                            System.out.println("The task: " + a.getName() + " has multiple to properties. This is probably an error.");
                        }

                        Artifact taskTo = getArtifactFromString(dependencyString.iterator().next());

                        if (taskTo != null) {
                            try {
                                Artifact dependency = (Artifact) dependencies.iterator().next().dependency;
                                Set<Dependency> artifactDependencies = forwardDependencyMap.get(dependency);
                                if (artifactDependencies == null) {
                                    artifactDependencies = new HashSet<Dependency>();
                                    forwardDependencyMap.put(dependency, artifactDependencies);
                                }
                                artifactDependencies.add(new Dependency(dependency, taskTo, dt));
                            } catch (ClassCastException e) {
                                System.out.println("Unable to map TASK_TO to an Artifact.");
                            }
                        }
                    }
                }

                if (!dependencies.isEmpty()) {
                    forwardDependencyMap.put(a, dependencies);
                }
            }
        }
        buildReverseDependencyMap(forwardDependencyMap);
        return forwardDependencyMap;
    }

    private void buildReverseDependencyMap(Map<Artifact, Set<Dependency>> forwardDependencyMap) {
        for (Map.Entry<Artifact, Set<Dependency>> entry : forwardDependencyMap.entrySet()) {
            for (Dependency d : entry.getValue()) {
                if (d.getDependency() instanceof Artifact) {
                    Artifact dependencyArtifact = (Artifact) d.getDependency();
                    Set<Dependency> reverseDependencySet = reverseDependencyMap.get(dependencyArtifact);
                    if (reverseDependencySet == null) {
                        reverseDependencySet = new HashSet<Dependency>();
                        reverseDependencyMap.put(dependencyArtifact, reverseDependencySet);
                    }
                    reverseDependencySet.add(d);
                }
            }
        }
    }

    /**
     * Gets a set of Dependencies for the given artifact.
     *
     * @param a
     * @param context
     * @param dependencyType
     * @return
     * @throws SaxonApiException
     */
    private Set<Dependency> getDepdendencySet(Artifact a, XdmNode context, Dependency.DependencyType dependencyType) throws SaxonApiException {
        Set<Dependency> dependencies = new HashSet<Dependency>();
        for (String dependencyString : evaluateXPathToStringSet(context, dependencyType.getXPath())) {
            Object dependencyObject = getArtifactFromString(dependencyString);
            if (dependencyObject == null) {
                dependencyObject = dependencyString;
            }

            dependencies.add(new Dependency(a, dependencyObject, dependencyType));
        }
        return dependencies;
    }

    /**
     * Returns an artifact resolved from a URL or null if no Artifact could be resolved.
     * @param str
     * @return
     */
    private Artifact getArtifactFromString(String str) {
        // The typical case: str is an artifact name
        Artifact dependency = artifactMap.get(str);

        if (dependency == null && str != null) {
            // If str is not an artifact name, it's probably a URI of some sort
            str = urifyString(str);
            try {
                URI uri = new URI(str);
                String scheme = uri.getScheme();
                if ("mailto".equals(scheme) || "vfs".equals(scheme)) {
                    return null;
                } else if ("http".equals(scheme) || "https".equals(scheme)) {
                    return getArtifactFromHttpUri(uri);
                } else if ("jms".equals(scheme)) {
                    return getArtifactFromJsmUri(uri);
                }
            } catch (URISyntaxException e) {
                System.out.println("Unparseable URI: " + str);
            }
        }

        return dependency;
    }

    /**
     * Under Windows the Maven build sometimes creates rather ugly (and invalid) file URIs.
     * @return
     */
    private String urifyString(String str) {
        // First replace any two subsequent backslashes with a single one
        str = str.replace("\\\\", "\\");
        // Then replace any backslashes with a slash
        return str.replace('\\', '/');
    }

    /**
     * Attemps to find a dependency by examining the URI path
     *
     * A URI of a proxy may take for instance the following forms:
     * http://localhost:9768/services/EventService.SOAP11Endpoint/
     * http://localhost:8280/services/MetadataLookup_queryProxy/GetAll
     *
     * JMS URIs look like:
     * jms:/TosUserToVleChangeQueueProxy?transport.jms.ConnectionFactoryJNDIName=QueueConnectionFactory&amp;java.naming.factory.initial=org.apache.activemq.jndi.ActiveMQInitialContextFactory&amp;transport.jms.DestinationType=queue&amp;java.naming.provider.url=tcp://localhost:61616
     * where the interesting bit is just the path part.
     *
     * @param uri
     * @return
     */
    private Artifact getArtifactFromHttpUri(URI uri) {
        String path = uri.getPath();
        String[] pathComponents = path.split("/");
        // Attempt to find an artifact from URL components
        for (String pathComponent : pathComponents) {
            Artifact a = artifactMap.get(pathComponent);
            if (a == null) {
                String[] componentParts = pathComponent.split("\\.");
                String artifactNameCandidate = null;
                for (String componentPart : componentParts) {
                    if (artifactNameCandidate == null) {
                        artifactNameCandidate = componentPart;
                    } else {
                        artifactNameCandidate += "." + componentPart;
                    }

                    a = artifactMap.get(artifactNameCandidate);

                    if (a != null) {
                        return a;
                    }
                }
            } else {
                return a;
            }
        }
        return null;
    }

    private Artifact getArtifactFromJsmUri(URI uri) {
        String artifactNameCandidate = uri.getPath().replace("/", "");
        return artifactMap.get(artifactNameCandidate);
    }

    /**
     * Returns a Set of Strings for the given XPath
     * @param context
     * @param xpath
     * @return
     * @throws SaxonApiException
     */
    private Set<String> evaluateXPathToStringSet(XdmNode context, XPathSelector xpath) throws SaxonApiException {
        Set<String> results = new HashSet<String>();
        xpath.setContextItem(context);
        for (XdmItem item : xpath) {
            String itemString = item.getStringValue();
            results.add(itemString);
        }
        return results;
    }

    /**
     * Represents a single Artifact. Supported artifact types are defined in the Artifact.ArtifactType enum.
     */
    public static class Artifact implements Comparable<Artifact> {

        private final String name;
        private final String version;
        private final ArtifactType type;
        private final String path;

        private final ArtifactDescription description;

        private final String carPath;

        public Artifact(String name, String version, ArtifactType type, String path, String carPath, ArtifactDescription description) {
            if (name == null || version == null || type == null || path == null || carPath == null) {
                throw new IllegalArgumentException("All Artifact constructor parameters except description must be non-null");
            }

            this.name = name;
            this.version = version;
            this.type = type;
            this.path = path;
            this.carPath = carPath;
            this.description = description;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }

        public ArtifactType getType() {
            return type;
        }

        /**
         * VFS path of this artifact within its car file
         * @return
         */
        public String getPath() {
            return path;
        }

        /**
         * VFS path of this artifact's car file
         * @return
         */
        public String getCarPath() {
            return carPath;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("{name: \"")
                    .append(name)
                    .append("\", version: \"")
                    .append(version)
                    .append("\", synapseType: \"")
                    .append(type)
                    .append("\", path: \"")
                    .append(path)
                    .append("\"}")
                    .toString();
        }

        @Override
        public int compareTo(Artifact artifact) {
            int difference = this.type.compareTo(artifact.type);
            if (difference == 0) {
                difference = this.name.compareTo(artifact.name);
                if (difference == 0) {
                    difference = this.version.compareTo(artifact.version);
                }
            }
            return difference;
        }

        @Override
        public boolean equals(Object o) {
            if (o != null && o instanceof Artifact) {
                Artifact other = (Artifact) o;

                return name.equals(other.name) && type.equals(other.type);
            }

            return false;
        }

        @Override
        public int hashCode() {
            int result = 37;

            result = 37 * result + name.hashCode();
            result = 37 * result + type.hashCode();

            return result;
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private static class ArtifactDescription {
            private String purpose;
            private ArtifactInterfaceInfo receives;
            private ArtifactInterfaceInfo returns;

            private ArtifactDescription(String purpose, ArtifactInterfaceInfo receives, ArtifactInterfaceInfo returns) {
                this.purpose = purpose;
                this.receives = receives;
                this.returns = returns;
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private static class ArtifactInterfaceInfo {
            private String description;
            private String example;

            private List<ArtifactIntefaceField> fields;

            public void addField(ArtifactIntefaceField field) {
                if (fields == null) {
                    fields = new ArrayList<ArtifactIntefaceField>();
                }
                fields.add(field);
            }
        }

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private static class ArtifactIntefaceField {
            private String description;
            private String path;
            private boolean optional = false;

            private ArtifactIntefaceField(String description, String path, boolean optional) {
                this.description = description;
                this.path = path;
                this.optional = optional;
            }
        }

        /**
         * Represents the different artifact types.
         */
        private enum ArtifactType {

            PROXY("synapse/proxy-service", "proxy"),
            SEQUENCE("synapse/sequence", "sequence"),
            ENDPOINT("synapse/endpoint", "endpoint"),
            API("synapse/api", "api"),
            MESSAGE_PROCESSOR("synapse/message-processors", "messageProcessor"),
            MESSAGE_STORE("synapse/message-store", "messageStore"),
            TASK("synapse/task", "task"),
            DATASERVICE("service/dataservice", "dataservice");

            private static final Map<String, ArtifactType> ARTIFACT_TYPE_MAP;

            static {
                ARTIFACT_TYPE_MAP = new HashMap<String, ArtifactType>(6);

                for (ArtifactType at : ArtifactType.values()) {
                    ARTIFACT_TYPE_MAP.put(at.synapseType, at);
                }
            }

            private final String synapseType;
            private final String typeString;

            ArtifactType(String synapseType, String typeString) {
                this.synapseType = synapseType;
                this.typeString = typeString;
            }

            public static ArtifactType getArtifactTypeByTypeString(String typeString) {
                return ARTIFACT_TYPE_MAP.get(typeString);
            }

            @Override
            public String toString() {
                return typeString;
            }
        }
    }

    /**
     * Represents the different dependency types between artifacts
     */
    public static class Dependency implements Comparable<Dependency> {

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
            STORE("store", STORE_XPATH_STRING),
            MESSAGE_PROCESSOR_STORE("messageStore", MESSAGE_PROCESSOR_STORE_XPATH_STRING),
            TASK_INJECT("inject", TASK_TARGET_XPATH_STRING),
            TASK_TO("taskTo", TASK_TO_XPATH_STRING);

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

                try {
                    this.xPath = COMPILER.compile(xPathUnion).load();
                } catch (SaxonApiException e) {
                    throw new Error("Unable to initialize the DependencyType enum. Unable to compile XPath.", e);
                }
            }

            @Override
            public String toString() {
                return typeString;
            }
        }

        private final Artifact dependent;
        private final Object dependency;
        private final DependencyType type;

        private Dependency(Artifact dependent, Object dependency, DependencyType dependencyType) {
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
            if (this.dependency instanceof Artifact && dependency.dependency instanceof Artifact ||
                    this.dependency instanceof String && dependency.dependency instanceof String) {
                return ((Comparable)this.dependency).compareTo((Comparable)dependency.dependency);
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
            String dependencyString = dependency instanceof Artifact ? ((Artifact)dependency).getName() : (String)dependency;
            return dependent.getName() + " -> " + dependencyString + " :[" + type + "]";
        }
    }
}
