package fi.mystes.esbdoc;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.*;
import static org.junit.Assert.*;
import static org.hamcrest.core.Is.*;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by jussi on 05/02/16.
 */
public class CarAnalyzerTest {

    private static final String DEFAULT_ESBDOC_RAW_PATH = "esbdoc-raw";
    private CarAnalyzer car;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

    }

    @Before
    public void setUp() throws Exception {
        car = new CarAnalyzer();
    }

    @After
    public void tearDown() throws Exception {

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

    }

    @Test
    public void testWithSingleProxy() throws Exception {
        String testName = "testWithSingleProxy";

        List<File> carFileList = new ArrayList<File>();
        carFileList.add(createCarFile(testName));

        File[] carFiles = carFileList.toArray(new File[carFileList.size()]);
        String esbdocRawPath = outputDestinationFor(testName);
        File[] soapUiFileSet = new File[0];

        car.run(carFiles, esbdocRawPath, soapUiFileSet);

        String esbDocSequencePath = sequencePathFor(esbdocRawPath);
        String esbDocMainModelPath = mainModelPathFor(esbdocRawPath);

        SequenceModelAssertion sequenceModelAssertion = new SequenceModelAssertion(esbDocSequencePath);
        sequenceModelAssertion.assertSize(1);
        sequenceModelAssertion.assertContains("Proxy");

        MainModelAssertion mainModelAssertion = new MainModelAssertion(esbDocMainModelPath);
        ProxyAssertion proxyAssertion = mainModelAssertion.proxyAssertionFor("Proxy");
        proxyAssertion.assertPurpose("Test ESBDoc with a single proxy");

        mainModelAssertion.assertNoTests();
        mainModelAssertion.assertNoForwardDependencies();
        mainModelAssertion.assertNoBackwardDependencies();
    }

    private String sequencePathFor(String esbdocRawPath){
        return esbdocRawPath + "-seq.json";
    }

    private String mainModelPathFor(String esbdocRawPath){
        return esbdocRawPath + ".json";
    }

    private class SequenceModelAssertion{
        private String jsonString;
        private JsonObject json;
        private JsonArray sequenceModels;

        private SequenceModelAssertion(){};

        public SequenceModelAssertion(String esbDocSequencePath) throws IOException {
            File sequenceFile = new File(esbDocSequencePath);
            assertTrue("File does not exist: " + esbDocSequencePath, sequenceFile.exists());

            this.jsonString = FileUtils.readFileToString(sequenceFile);
            this.json = new Gson().fromJson(this.jsonString, JsonObject.class);
            assertTrue("File does not contain expected element: models", json.has("models"));

            JsonObject models = json.getAsJsonObject("models");
            assertTrue("models-element does not contain expected element: sequence-models", models.has("sequence-models"));

            this.sequenceModels = models.getAsJsonArray("sequence-models");
            assertNotNull("sequence-models element is null and that's not OK.", this.sequenceModels);
        }

        public void assertSize(int expected){
            assertThat(this.sequenceModels.size(), is(expected));
        }

        public void assertContains(String expected){
            boolean matchFound = false;
            Iterator<JsonElement> iterator = this.sequenceModels.iterator();
            while(iterator.hasNext()){
                JsonObject currentElement = iterator.next().getAsJsonObject();
                String actual = currentElement.get("name").getAsString();
                if(StringUtils.equals(expected, actual)){
                    matchFound = true;
                }
            }

            assertTrue(matchFound);
        }

    }

    private class MainModelAssertion {
        private String jsonString;
        private JsonObject json;
        private Set<Map.Entry<String, JsonElement>> resources;
        private Set<Map.Entry<String, JsonElement>> dependencies;
        private Set<Map.Entry<String, JsonElement>> tests;

        private MainModelAssertion(){};

        public MainModelAssertion(String esbDocProxyPath) throws IOException {
            File proxyFile = new File(esbDocProxyPath);
            assertTrue("File does not exist: " + esbDocProxyPath, proxyFile.exists());

            this.jsonString = FileUtils.readFileToString(proxyFile);
            this.json = new Gson().fromJson(this.jsonString, JsonObject.class);

            assertTrue("File does not contain expected element: resources", json.has("resources"));
            assertTrue("File does not contain expected element: dependencies", json.has("dependencies"));
            assertTrue("File does not contain expected element: tests", json.has("tests"));

            this.resources = json.get("resources").getAsJsonObject().entrySet();
            this.dependencies = json.get("dependencies").getAsJsonObject().entrySet();
            this.tests = json.get("tests").getAsJsonObject().entrySet();

            assertNotNull("resource set is null and that's not OK.", this.resources);
            assertNotNull("dependency set is null and that's not OK.", this.dependencies);
            assertNotNull("test set is null and that's not OK.", this.tests);
        }

        public void assertNoTests(){
            assertThat(this.tests.size(), is(0));
        }

        public void assertNoForwardDependencies() {
        }

        public void assertNoBackwardDependencies() {
        }

        public ProxyAssertion proxyAssertionFor(String proxyName) {
            return new ProxyAssertion(this.resources, proxyName);
        }
    }
    private enum DependencyType {
        FORWARD, BACKWARD
    }

    private class DependencyTypeAssertion<DependencyType> {

        private Set<Map.Entry<String, JsonElement>> dependencies;

        public DependencyTypeAssertion(Set<Map.Entry<String, JsonElement>> dependencies){
            
        }
    }

    private class ProxyAssertion {
        private JsonObject proxy;

        public ProxyAssertion(Set<Map.Entry<String, JsonElement>> resources, String name){

            boolean proxyFound = false;
            for(Map.Entry<String, JsonElement> resource : resources){
                if(StringUtils.equals(name, resource.getKey())){
                    JsonObject proxyCandidate = resource.getValue().getAsJsonObject();
                    assertThat(proxyCandidate.get("type").getAsString(), is("proxy"));
                    proxyFound = true;
                    this.proxy = proxyCandidate;
                }
            }
            assertTrue(proxyFound);
        }

        public void assertPurpose(String expected){
            String actual = this.proxy.get("purpose").getAsString();
            assertThat(actual, is(expected));
        }
    }

    private String outputDestinationFor(String testName){
        URL resourceUrl = CarAnalyzer.class.getResource("/");
        return resourceUrl.getPath() + "/" + testName + "/" + DEFAULT_ESBDOC_RAW_PATH;
    }

    private File createCarFile(String testName) throws Exception {
        URL resourceUrl = CarAnalyzer.class.getResource("/" + testName);

        String sourceFolder = resourceUrl.getPath();
        String targetArchive = sourceFolder + "/" + testName + ".car";

        zipFolder(sourceFolder, targetArchive);

        return new File(targetArchive);
    }

    private static void zipFolder(String sourceFolderPath, String targetArchive) throws Exception {
        FileOutputStream fileWriter = new FileOutputStream(targetArchive);
        ZipOutputStream zip = new ZipOutputStream(fileWriter);

        addRootLevelToZip(sourceFolderPath, zip, targetArchive);

        zip.flush();
        zip.close();
    }

    private static void addRootLevelToZip(String physicalPath, ZipOutputStream zip, String targetArchive) throws Exception {
        File folder = new File(physicalPath);

        targetArchive = StringUtils.remove(targetArchive, physicalPath);
        targetArchive = targetArchive.startsWith("/") ? StringUtils.remove(targetArchive, "/") : targetArchive;
        String[] fileNames = folder.list();
        fileNames = ArrayUtils.removeElement(fileNames, targetArchive);

        for (String fileName : fileNames) {
            addToZip("", physicalPath + "/" + fileName, zip);
        }
    }

    private static void addFolderToZip(String logicalPath, String physicalPath, ZipOutputStream zip) throws Exception {
        File folder = new File(physicalPath);

        for (String fileName : folder.list()) {
            String currentLogicalPath = StringUtils.equals(logicalPath, "") ? folder.getName() : logicalPath + "/" + folder.getName();
            addToZip(currentLogicalPath, physicalPath + "/" + fileName, zip);
        }
    }

    private static void addToZip(String logicalPath, String physicalPath, ZipOutputStream zip) throws Exception {

        File folder = new File(physicalPath);
        if (folder.isDirectory()) {
            addFolderToZip(logicalPath, physicalPath, zip);
        } else {
            addFileToZip(logicalPath, physicalPath, zip);
        }
    }

    private static void addFileToZip(String logicalPath, String physicalPath, ZipOutputStream zip) throws IOException {
        File folder = new File(physicalPath);
        FileInputStream in = new FileInputStream(physicalPath);
        zip.putNextEntry(new ZipEntry(logicalPath + "/" + folder.getName()));

        byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) > 0) {
            zip.write(buffer, 0, length);
        }
    }
}
