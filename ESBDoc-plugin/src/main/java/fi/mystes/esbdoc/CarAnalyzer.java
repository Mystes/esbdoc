package fi.mystes.esbdoc;

import net.sf.saxon.s9api.*;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMXMLBuilderFactory;
import org.apache.axiom.om.impl.llom.OMElementImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.jaxen.JaxenException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;

import static fi.mystes.esbdoc.Constants.*;

/**
 * Created by Jarkko on 1.10.2014.
 */
public class CarAnalyzer {

    private static Log log = LogFactory.getLog(CarAnalyzer.class);

    private ArtifactMap artifactMap = new ArtifactMap();
    private ArtifactDependencyMap forwardDependencyMap = new ArtifactDependencyMap();
    private ArtifactDependencyMap reverseDependencyMap = new ArtifactDependencyMap();
    private TestMap testsMap = new TestMap();
    private List<String> forbiddenArtifactNames = new ArrayList<String>(Arrays.asList("services"));
    private SortedMap<String, String> servicePathMap = new TreeMap<String, String>();

    private FileSystemManager fileSystemManager;
    private String currentObject = null; // Used for information logging if sequence or proxy has  invalid fields

    public CarAnalyzer() throws FileSystemException, ParserConfigurationException, JaxenException {
            fileSystemManager = VFS.getManager();
    }

    /**
     * The purpose of 'main' is to allow running this analyzer independently, from the command line.
     * Its logic should not be different from running the analyzer as a part of Maven Mojo execution, using 'run'.
     * Therefore 'main' should delegate execution to 'run' as early as possible.
     */
    public static void main(String[] args) throws IOException, SaxonApiException, ParserConfigurationException, SAXException, XPathExpressionException, JaxenException {
        log.info("Running...");
        if (CommandLineArguments.from(args).areNotOk()) {
            return;
        }

        String commonPartOfOutputFilename = CommandLineArguments.getCommonPartOfOutputFilename();
        String commaSeparatedListOfCarFilenames = CommandLineArguments.getCommaSeparatedListOfCarFilenames();
        String commaSeparatedListOfSoapUiFolderNames = CommandLineArguments.getCommaSeparatedListOfSoapUiFolderNames();

        File[] carFiles = Files.convertToFileHandles(commaSeparatedListOfCarFilenames);
        File[] testFolders = Files.convertToFileHandles(commaSeparatedListOfSoapUiFolderNames);

        new CarAnalyzer().run(carFiles, commonPartOfOutputFilename, testFolders);

        log.info("Done!");
    }

    public void run(File[] carFiles, String outputDestination, File[] testFolders) throws IOException, SaxonApiException, ParserConfigurationException, SAXException, XPathExpressionException, JaxenException {
        List<FileObject> carFileObjects = Files.getCarFileObjects(carFiles);
        List<FileObject> testFileObjects = Files.getTestFileObjects(testFolders);
        processFileObjects(carFileObjects, outputDestination, testFileObjects);
    }

    private void processFileObjects(List<FileObject> carFileObjects, String outputDestination, List<FileObject> testFileObjects) throws IOException, SaxonApiException, ParserConfigurationException, SAXException, XPathExpressionException, JaxenException {
        getArtifactMap(carFileObjects);
        getForwardDependencyMap();
        buildTestFileMap(testFileObjects);
        // Process sequence diagrams
        Map<String, SequenceItem> seqs = SequenceDiagramBuilder.instance().getSequenceItemMap();
        writeOutputFiles(outputDestination);
        SequenceDiagramBuilder.instance().writeOutputFile(outputDestination);
    }

    private void writeOutputFiles(String outputFilename) throws IOException {
        Files.buildDirectoryPathFor(outputFilename);
        Files.writeTextTo(outputFilename, forwardDependencyMap.toDependencyStrings());

        FileOutputStream jsonStream = Files.jsonOutputFor(outputFilename);
        new JsonWriter(forwardDependencyMap, reverseDependencyMap, testsMap, artifactMap).writeJson(jsonStream);
        jsonStream.close();
    }


    /**
     * Returns a map mapping artifact names to the artifacts in the car files
     * pointed to by the given FileObjects
     *
     * @param carFileObjects
     * @return
     * @throws IOException
     * @throws SaxonApiException
     */
    private SortedMap<String, Artifact> getArtifactMap(List<FileObject> carFileObjects) throws IOException, SaxonApiException, SAXException, XPathExpressionException, JaxenException {
        for (FileObject carFileObject : carFileObjects) {
            FileObject artifactsFileObject = carFileObject.getChild("artifacts.xml");
            log.info(MessageFormat.format("Processing artifacts.xml file: [{0}]", artifactsFileObject.getURL().toString()));
            XdmValue value = SaxonXPath.apply(DEPENDENCY_XPATH_STRING).to(artifactsFileObject).andReturnAnXdmValue();
            for (XdmItem item : value) {
                Artifact artifact = getArtifact((XdmNode) item, carFileObject);
                if (artifact != null) {
                    artifactMap.put(artifact.getName(), artifact);
                }
            }
        }
        return artifactMap;
    }

    /**
     * Returns a map mapping artifact names to the SoapUI tests in given
     * FileObjects
     *
     * @param testFileObjects
     * @return
     * @throws IOException
     * @throws SaxonApiException
     */
    private SortedMap<String, Set<TestProject>> buildTestFileMap(List<FileObject> testFileObjects) throws IOException, SaxonApiException, SAXException, XPathExpressionException, JaxenException {
        if (testFileObjects != null) {
            for (FileObject testFileObject : testFileObjects) {
                log.info(MessageFormat.format("Processing SoapUI file: [{0}]", testFileObject.getURL().toString()));

                XdmValue value = SaxonXPath.apply(TESTCASE_XPATH_STRING).to(testFileObject).andReturnAnXdmValue();
                XdmNode rootElement = (XdmNode) value.itemAt(0);

                //find artifacts from the file and map them to TestCases and TestSuites
                SortedMap<String, Set<TestSuite>> testSuiteMap = buildTestSuiteMap(rootElement);

                // iterate test suites for every artifact and add it to testsMap
                for (String artifact : testSuiteMap.keySet()) {
                    TestProject project = new TestProject(rootElement.getAttributeValue(NAME_Q), testFileObject.getName().getBaseName(), testSuiteMap.get(artifact));
                    addTestProjectForArtifact(artifact, project);

                    // Add also test references to all forward dependencies
                    // List is created to keep track that we are adding references to certain artifact only once
                    List<String> artifactList = new ArrayList<String>();
                    addTestsToForwardDependencies(artifact, project, artifactList);
                }
            }
        }
        return testsMap;
    }

    /**
     * Method gets a SoapUI project root node as argument and method finds all
     * references to artifacts.
     *
     * @param soapUIProjectRoot
     * @return SortedMap where key is artifact name and value is set of
     * TestSuite -objects
     * @throws IOException
     * @throws SaxonApiException
     */
    private SortedMap<String, Set<TestSuite>> buildTestSuiteMap(XdmNode soapUIProjectRoot) throws IOException, SaxonApiException {
        SortedMap<String, String> testProjectPropertiesMap = new TreeMap<String, String>();
        processProperties(soapUIProjectRoot, testProjectPropertiesMap, TestItemType.PROJECT);

        XdmSequenceIterator testSuites = soapUIProjectRoot.axisIterator(Axis.DESCENDANT, SOAPUI_TEST_SUITE_Q);
        SortedMap<String, Set<TestSuite>> testSuiteMap = new TreeMap<String, Set<TestSuite>>();

        // let's start going through all the test suites in project file
        while (testSuites.hasNext()) {
            XdmItem testSuite = testSuites.next();

            if (testSuite instanceof XdmNode) {
                XdmNode testSuiteNode = (XdmNode) testSuite;
                SortedMap<String, String> testSuitePropertiesMap = new TreeMap<String, String>(testProjectPropertiesMap);

                processProperties(testSuiteNode, testSuitePropertiesMap, TestItemType.TEST_SUITE);

                // iterate through all the test cases under test suite
                XdmSequenceIterator testCases = testSuiteNode.axisIterator(Axis.DESCENDANT, SOAPUI_TEST_CASE_Q);
                SortedMap<String, Set<TestCase>> testCaseMap = new TreeMap<String, Set<TestCase>>();

                while (testCases.hasNext()) {
                    XdmItem testCase = testCases.next();
                    if (testCase instanceof XdmNode) {
                        XdmNode testCaseNode = (XdmNode) testCase;

                        // iterate through all the test steps under test cases
                        XdmSequenceIterator testSteps = testCaseNode.axisIterator(Axis.DESCENDANT, SOAPUI_TEST_STEP_Q);
                        while (testSteps.hasNext()) {
                            XdmItem testStep = testSteps.next();
                            if (testStep instanceof XdmNode) {
                                XdmNode testStepNode = (XdmNode) testStep;

                                // if endpoint found and it matches an artifact, add it to the map
                                XdmSequenceIterator endpoints = testStepNode.axisIterator(Axis.DESCENDANT, SOAPUI_ENDPOINT_Q);
                                if (endpoints.hasNext()) {
                                    SortedMap<String, String> testCasePropertiesMap = new TreeMap<String, String>();

                                    // combine project, test suite and test case properties
                                    processProperties(testCaseNode, testCasePropertiesMap, TestItemType.TEST_CASE);
                                    testCasePropertiesMap.putAll(testSuitePropertiesMap);

                                    XdmNode endpoint = (XdmNode) endpoints.next();
                                    if (endpoint.getNodeKind() == XdmNodeKind.ELEMENT) {

                                        String propertyHandledEndpoint = replaceTestPropertyInUrl(endpoint.getStringValue(), testCasePropertiesMap);
                                        // if it is a REST api call it might have resourcePath information in other element too
                                        XdmSequenceIterator config = testStepNode.axisIterator(Axis.DESCENDANT, SOAPUI_CONFIG_Q);
                                        if (config.hasNext()) {
                                            XdmNode configNode = (XdmNode) config.next();
                                            if (configNode.getAttributeValue(SOAPUI_RESOURCE_PATH_Q) != null) {
                                                String resourcePath = configNode.getAttributeValue(SOAPUI_RESOURCE_PATH_Q);
                                                propertyHandledEndpoint += resourcePath;
                                            }
                                        }

                                        Artifact fact = getArtifactFromString(propertyHandledEndpoint);
                                        if (fact != null) {
                                            TestCase c = new TestCase(testCaseNode.getAttributeValue(NAME_Q));
                                            if (!testCaseMap.containsKey(fact.getName())) {
                                                HashSet<TestCase> cases = new HashSet<TestCase>();
                                                testCaseMap.put(fact.getName(), cases);
                                            }
                                            testCaseMap.get(fact.getName()).add(c);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Go through all the test cases that were found for artifacts under current test suite, create a test
                // suite entity for each artifact and add it to the test suite map for each artifact
                if (!testCaseMap.isEmpty()) {
                    for (String artifact : testCaseMap.keySet()) {
                        if (!testSuiteMap.containsKey(artifact)) {
                            HashSet<TestSuite> suites = new HashSet<TestSuite>();
                            testSuiteMap.put(artifact, suites);
                        }
                        String name = ((XdmNode) testSuite).getAttributeValue(NAME_Q);
                        testSuiteMap.get(artifact).add(new TestSuite(name, testCaseMap.get(artifact)));
                    }
                }
            }
        }

        return testSuiteMap;
    }

    /**
     * This method receives node that has properties-node child (soapui-project,
     * testSuite or testCase). Method goes through all property elements and add
     * keys and values to given map
     *
     * @param propertiesParent
     * @param currentPropertiesMap
     * @param type
     */
    private void processProperties(XdmNode propertiesParent, SortedMap<String, String> currentPropertiesMap, TestItemType type) {
        if (propertiesParent.axisIterator(Axis.CHILD, SOAPUI_PROPERTIES_Q).hasNext()) {
            XdmNode propertiesNode = (XdmNode) propertiesParent.axisIterator(Axis.CHILD, SOAPUI_PROPERTIES_Q).next();

            XdmSequenceIterator propertyNodes = propertiesNode.axisIterator(Axis.CHILD, SOAPUI_PROPERTY_Q);

            while (propertyNodes.hasNext()) {
                XdmNode property = (XdmNode) propertyNodes.next();

                XdmNode name = getChild(property, SOAPUI_NAME_Q);
                XdmNode value = getChild(property, SOAPUI_VALUE_Q);

                if (value != null) {
                    String key;
                    if (TestItemType.TEST_CASE == type) {
                        key = "${#TestCase#" + name.getStringValue() + "}";
                    } else if (TestItemType.TEST_SUITE == type) {
                        key = "${#TestSuite#" + name.getStringValue() + "}";
                    } else {
                        key = "${#Project#" + name.getStringValue() + "}";
                    }

                    currentPropertiesMap.put(key, value.getStringValue());
                }
            }
        }
    }

    private String replaceTestPropertyInUrl(String url, SortedMap<String, String> propertiesMap) {
        if (url.contains("${")) {
            String key = url.substring(url.indexOf("${"), url.indexOf("}") + 1);
            String value = propertiesMap.get(key);
            String newUrl = url.replace(key, value);
            return newUrl;
        }
        return url;
    }

    private XdmNode getChild(XdmNode parent, QName childName) {
        XdmSequenceIterator xdmSequenceIterator = parent.axisIterator(Axis.CHILD, childName);
        if (xdmSequenceIterator.hasNext()) {
            return (XdmNode) xdmSequenceIterator.next();
        } else {
            return null;
        }
    }

    /**
     * Adds given TestProject to all forward dependencies of given artifact.
     *
     * @param artifactName
     * @param project
     * @param artifactList recursion keeps track of added artifacts with given
     * list. Artifact is only processed if it has not been processed before (=
     * not in the given list)
     */
    private void addTestsToForwardDependencies(String artifactName, TestProject project, List<String> artifactList) {
        Artifact artifact = getArtifactFromString(artifactName);
        if (forwardDependencyMap.containsKey(artifact)) {
            for (Dependency d : forwardDependencyMap.get(artifact)) {
                if (d.getDependency() instanceof Artifact) {
                    Artifact a = (Artifact) d.getDependency();
                    if (!artifactList.contains(a.getName())) {
                        addTestProjectForArtifact(a.getName(), project);
                        artifactList.add(a.getName());
                        addTestsToForwardDependencies(a.getName(), project, artifactList);
                    }
                }
            }
        }
    }

    /**
     * Adds given TestProject for artifact to the testsMap
     *
     * @param artifact
     * @param project
     */
    private void addTestProjectForArtifact(String artifact, TestProject project) {
        if (!testsMap.containsKey(artifact)) {
            HashSet<TestProject> projects = new HashSet<TestProject>();
            testsMap.put(artifact, projects);
        }
        testsMap.get(artifact).add(project);
    }

    /**
     * Returns an XdmNode pointing to the root element of the XML in the
     * specified file
     *
     * @param xmlFo
     * @return
     * @throws SaxonApiException
     * @throws IOException
     */
    private XdmNode getNodeFromFileObject(FileObject xmlFo) throws SaxonApiException, IOException {
        InputStream is = null;
        InputStream isSeq = null; // This stream is used for sequenceDiagram generation
        try {
            is = xmlFo.getContent().getInputStream();
            if (xmlFo.getName().getBaseName().indexOf("-soapui-project.xml") == -1) {
                isSeq = xmlFo.getContent().getInputStream();
                String seg = SequenceDiagramBuilder.instance().buildPipe(isSeq);
                isSeq.close();
            }
            XdmNode xmlNode = SaxonUtil.readToNode(is);
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

    /**
     * Returns an Artifact object for the given XdmNode containing an artifact
     * definition from an artifact.xml file
     *
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
        String artifactFilePath = SaxonXPath.apply(ARTIFACT_FILENAME_XPATH_STRING).to(artifactFileXml).andReturnA(String.class);
        String artifactTypeString = artifactFileXml.getAttributeValue(TYPE_Q);

        Artifact.ArtifactType artifactType = Artifact.ArtifactType.getArtifactTypeByTypeString(artifactTypeString);

        String artifactName = null;
        if (artifactType == Artifact.ArtifactType.RESOURCE) {
            artifactName = dependencyName;
        } else {
            artifactName = getRealNameForArtifact(carFile.toString() + dependencyDirectory + artifactFilePath);
        }

        getServicePath(artifactType, artifactName, carFile.toString() + dependencyDirectory + artifactFilePath);

        Artifact.ArtifactDescription description = getArtifactDescription(carFile.toString() + dependencyDirectory + artifactFilePath);

        if (artifactType == null && !IGNORED_ARTIFACT_TYPES.contains(artifactTypeString)) {
            log.warn("Unrecognized artifact type: " + artifactTypeString);
            return null;
        } else if (IGNORED_ARTIFACT_TYPES.contains(artifactTypeString)) {
            return null;
        }

        // some artifact names might be forbidden. Like in some projects there is a "services" resources in use in registry.
        // That name is bad for url parsing because "services" is found in every url
        if (forbiddenArtifactNames.contains(artifactName)) {
            return null;
        }

        return new Artifact(artifactName, dependencyVersion, artifactType, dependencyDirectory + artifactFilePath, carFile.toString(), description);

    }

    private void getServicePath(Artifact.ArtifactType artifactType, String artifactName, String artifactFilePath) throws FileSystemException, JaxenException {
        FileObject artifactFileObject = fileSystemManager.resolveFile(artifactFilePath);

        OMElement root = OMXMLBuilderFactory.createOMBuilder(artifactFileObject.getContent().getInputStream()).getDocumentElement();

        if (artifactType == Artifact.ArtifactType.API) {
            String context = root.getAttributeValue(CONTEXT_Q);
            //String urlMapping = root.getAttributeValue(URL_MAPPING_Q);
            Iterator resourceElements = root.getChildrenWithName(RESOURCE_Q);
            while (resourceElements.hasNext()) {
                OMElement resourceElement = (OMElement) resourceElements.next();
                String urlMapping = resourceElement.getAttributeValue(URL_MAPPING_Q);
                String path = context + urlMapping;
                servicePathMap.put(path, artifactName);
            }
        }
    }

    private Artifact.ArtifactDescription getArtifactDescription(String artifactFilePath) throws IOException, JaxenException {
        FileObject artifactFileObject = fileSystemManager.resolveFile(artifactFilePath);

        OMElement root = OMXMLBuilderFactory.createOMBuilder(artifactFileObject.getContent().getInputStream()).getDocumentElement();

        Object evaluationResult = SynapseXPath.evaluateOmElement(root);
        if (evaluationResult instanceof List) {
            List resultList = (List) evaluationResult;

            if (!resultList.isEmpty()) {
                OMElement descriptionElement = (OMElement) resultList.get(0);
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

                    OMElement dependeciesElement = descriptionElement.getFirstChildWithName(DEPENDENCIES_Q);
                    List<String> programmerDefinedDependencies = null;
                    if (dependeciesElement != null) {
                        Iterator gator = dependeciesElement.getChildren();
                        programmerDefinedDependencies = new ArrayList<String>();
                        while (gator.hasNext()) {
                            Object element = gator.next();
                            if (element instanceof OMElementImpl) {
                                OMElementImpl dependencyElement = (OMElementImpl)element;
                                String dependencyName = dependencyElement.getText();
                                programmerDefinedDependencies.add(dependencyName);
                            }
                        }
                    }

                    if (purpose != null || receives != null || returns != null || programmerDefinedDependencies != null) {
                        return new Artifact.ArtifactDescription(purpose, receives, returns, programmerDefinedDependencies);
                    }
                }
            }
        }

        return null;
    }

    private Artifact.ArtifactInterfaceInfo getArtifactInterfaceInfo(OMElement infoElement) {
        Artifact.ArtifactInterfaceInfo artifactInterfaceInfo = new Artifact.ArtifactInterfaceInfo();
        String description = infoElement.getText();
        if (description != null) {
            description = description.trim();

            if (!description.isEmpty()) {
                artifactInterfaceInfo.description = description;
            }
        }

        OMElement exampleElement = infoElement.getFirstChildWithName(EXAMPLE_Q);
        if (exampleElement != null) {
            String content = null;
            if (exampleElement.getFirstElement() != null) {
                content = exampleElement.getFirstElement().toString();
            } else if(exampleElement.getText() != null) {
                content = exampleElement.getText();
            }

            if (content != null && !content.isEmpty()) {
                String example = content.trim();

                if (!example.isEmpty()) {
                    artifactInterfaceInfo.example = example;
                }
                //} else if ()
            }
        }

        Iterator<OMElement> fields = infoElement.getChildrenWithName(FIELD_Q);
        while (fields.hasNext()) {
            OMElement field = fields.next();

            String fieldDescription = field.getAttributeValue(DESCRIPTION_Q);
            String fieldPath = field.getAttributeValue(PATH_Q);
            boolean isOptional = "true".equals(field.getAttributeValue(OPTIONAL_Q));

            if (fieldPath != null) {
                artifactInterfaceInfo.addField(new Artifact.ArtifactIntefaceField(fieldDescription, fieldPath, isOptional));
            }
        }

        if (artifactInterfaceInfo.description != null || artifactInterfaceInfo.fields != null) {
            return artifactInterfaceInfo;
        }

        return null;
    }

    /**
     * Returns the name of the artifact defined in the given file
     *
     * @param artifactFilePath
     * @return
     * @throws IOException
     * @throws SaxonApiException
     */
    private String getRealNameForArtifact(String artifactFilePath) throws IOException, SaxonApiException {
        FileObject artifactFileObject = fileSystemManager.resolveFile(artifactFilePath);
        XdmNode artifactXml = getNodeFromFileObject(artifactFileObject);
        return artifactXml.getAttributeValue(NAME_Q);
    }

    /**
     * Returns a map mapping artifacts to their direct dependencies
     *
     * @return
     * @throws SaxonApiException
     * @throws IOException
     */
    private SortedMap<Artifact, Set<Dependency>> getForwardDependencyMap() throws SaxonApiException, IOException {
        for (Artifact artifact : artifactMap.values()) {
            FileObject artifactFileObject = fileSystemManager.resolveFile(artifact.getCarPath() + artifact.getPath());
            if (!artifactFileObject.exists()) {
                log.warn("File does not exist: " + artifactFileObject.toString());
            } else {
                XdmNode artifactXml = getNodeFromFileObject(artifactFileObject);
                log.debug(artifactFileObject.toString() + " contains the following XML: " + artifactXml.toString());

                Set<Dependency> dependencies = new HashSet<Dependency>();

                for (DependencyType dependencyType : DependencyType.values()) {
                    // TASK_TO has special handling
                    if (dependencyType == DependencyType.TASK_TO) {
                        continue;
                    }

                    log.debug("Adding dependencies of type " + dependencyType);
                    dependencies.addAll(getDependencySet(artifact, artifactXml, dependencyType));
                }

                log.debug("Artifact type is: " + artifact.getType());
                log.debug("Dependencies are empty: " + dependencies.isEmpty());

                if (artifact.getType() == Artifact.ArtifactType.TASK && !dependencies.isEmpty()) {
                    // A task should only have artifact single dependency, artifact proxy or artifact sequence
                    if (dependencies.size() > 1) {
                        System.out.println("The task: " + artifact.getName() + " has multiple dependencies. This is probably an error.");
                    }

                    DependencyType dt = DependencyType.TASK_TO;
                    Set<String> dependencyString = evaluateXPathToStringSet(artifactXml, dt.getXPath());

                    if (dependencyString != null && !dependencyString.isEmpty()) {
                        if (dependencyString.size() > 1) {
                            System.out.println("The task: " + artifact.getName() + " has multiple to properties. This is probably an error.");
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
                    forwardDependencyMap.put(artifact, dependencies);
                }
            }

            // add description defined dependencies
            if (artifact.description != null && artifact.description.dependencies != null) {
                List<String> dependencies = artifact.description.dependencies;
                if (!dependencies.isEmpty()) {
                    for (String dependencyName : dependencies) {
                        Artifact dependency = getArtifactFromString(dependencyName);
                        Set<Dependency> artifactDependencies = forwardDependencyMap.get(artifact);
                        if (artifactDependencies == null) {
                            artifactDependencies = new HashSet<Dependency>();
                            forwardDependencyMap.put(artifact, artifactDependencies);
                        }
                        artifactDependencies.add(new Dependency(artifact, dependency, DependencyType.DOCUMENTED));
                    }
                }
            }
        }
        buildReverseDependencyMap(forwardDependencyMap);
        return forwardDependencyMap;
    }

    private void buildReverseDependencyMap(Map<Artifact, Set<Dependency>> forwardDependencyMap) {
        for (Map.Entry<Artifact, Set<Dependency>> entry : forwardDependencyMap.entrySet()) {
            for (Dependency dependency : entry.getValue()) {
                if (dependency.getDependency() instanceof Artifact) {
                    Artifact dependencyArtifact = (Artifact) dependency.getDependency();
                    Set<Dependency> reverseDependencySet = reverseDependencyMap.get(dependencyArtifact);
                    if (reverseDependencySet == null) {
                        reverseDependencySet = new HashSet<Dependency>();
                        reverseDependencyMap.put(dependencyArtifact, reverseDependencySet);
                    }
                    reverseDependencySet.add(dependency);
                }
            }
        }
    }

    /**
     * Gets artifact set of Dependencies for the given artifact.
     *
     * @param artifact
     * @param context
     * @param dependencyType
     * @return
     * @throws SaxonApiException
     */
    private Set<Dependency> getDependencySet(Artifact artifact, XdmNode context, DependencyType dependencyType) throws SaxonApiException {
        Set<Dependency> dependencies = new HashSet<Dependency>();
        for (String dependencyString : evaluateXPathToStringSet(context, dependencyType.getXPath())) {
            currentObject = artifact.getName(); // Save current artifact for  warning logs.
            Object dependencyObject = getArtifactFromString(dependencyString);
            if (dependencyObject == null) {
                dependencyObject = dependencyString;
            }

            dependencies.add(new Dependency(artifact, dependencyObject, dependencyType));
        }
        return dependencies;
    }

    /**
     * Returns an artifact resolved from a URL or null if no Artifact could be
     * resolved.
     *
     * @param string
     * @return
     */
    private Artifact getArtifactFromString(String string) {
        // The typical case: string is an artifact name
        Artifact dependency = artifactMap.get(string);

        if (dependency == null && string != null) {
            // If string is not an artifact name, it's probably a URI of some sort
            string = urifyString(string);
            try {
                URI uri = new URI(string);
                String scheme = uri.getScheme();
                if ("mailto".equals(scheme) || "vfs".equals(scheme)) {
                    return null;
                } else if ("http".equals(scheme) || "https".equals(scheme)) {
                    return getArtifactFromHttpUri(uri);
                } else if ("jms".equals(scheme)) {
                    return getArtifactFromJmsUri(uri);
                } else if ("gov".equals(scheme) || "conf".equals(scheme)) {
                    return getArtifactFromRegistyUri(uri);
                } else {
                    log.warn(currentObject + "Unrecognized URI scheme for URI: " + uri.toString());
                }
            } catch (URISyntaxException e) {
                log.warn(currentObject + "Unparseable URI: " + string);
            }
        }

        return dependency;
    }

    /**
     * Under Windows the Maven build sometimes creates rather ugly (and invalid)
     * file URIs.
     *
     * @return
     */
    private String urifyString(String string) {
        // First replace any two subsequent backslashes with a single one
        string = string.replace("\\\\", "\\");
        // Then replace any backslashes with a slash
        return string.replace('\\', '/');
    }

    private Artifact getArtifactFromRegistyUri(URI uri) {
        return getArtifactFromPath(uri.getSchemeSpecificPart());
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
        return getArtifactFromPath(uri.getPath());
    }

    /**
     * Attemps to resolve an Artifact's name from a path String one path element
     * at a time
     *
     * @param path
     * @return
     */
    private Artifact getArtifactFromPath(String path) {
        // path might be in the service path map
        if (servicePathMap.containsKey(path)) {
            String artifactName = servicePathMap.get(path);
            Artifact artifact = artifactMap.get(artifactName);
            if (artifact != null) {
                return artifact;
            }
        }

        String[] pathComponents = path.split("/");
        // Attempt to find an artifact from URL components
        for (String pathComponent : pathComponents) {
            Artifact artifact = artifactMap.get(pathComponent);
            if (artifact == null) {
                String[] componentParts = pathComponent.split("\\.");
                String artifactNameCandidate = null;
                for (String componentPart : componentParts) {
                    if (artifactNameCandidate == null) {
                        artifactNameCandidate = componentPart;
                    } else {
                        artifactNameCandidate += "." + componentPart;
                    }

                    artifact = artifactMap.get(artifactNameCandidate);

                    if (artifact != null) {
                        return artifact;
                    }
                }
            } else {
                return artifact;
            }
        }
        return null;
    }

    private Artifact getArtifactFromJmsUri(URI uri) {
        String artifactNameCandidate = uri.getPath().replace("/", "");
        return artifactMap.get(artifactNameCandidate);
    }

    /**
     * Returns a Set of Strings for the given XPath
     *
     * @param context
     * @param xpath
     * @return
     * @throws SaxonApiException
     */
    private Set<String> evaluateXPathToStringSet(XdmNode context, XPathSelector xpath) throws SaxonApiException {
        if(null == xpath){
            log.warn("Cannot apply XPath to Context: XPath is null! Returning empty HashSet...");
            return new HashSet<String>();
        }

        if(null == context){
            log.warn("Cannot apply XPath to Context: Context is null! Returning empty HashSet...");
            return new HashSet<String>();
        }

        return SaxonXPath.apply(xpath).to(context).andReturnASetOf(String.class);
    }

}
