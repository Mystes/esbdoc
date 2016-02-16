package fi.mystes.esbdoc;

import org.junit.*;

import java.io.*;
import java.net.URL;
import java.util.*;

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

        mainModelAssertion.assertNoTests();
        mainModelAssertion.assertNoDependencies();

        ProxyAssertion proxyAssertion = mainModelAssertion.proxyAssertionFor("Proxy");
        proxyAssertion.assertPurpose("Test ESBDoc with a single proxy");
    }

    private String sequencePathFor(String esbdocRawPath){
        return esbdocRawPath + "-seq.json";
    }

    private String mainModelPathFor(String esbdocRawPath){
        return esbdocRawPath + ".json";
    }

    private String outputDestinationFor(String testName){
        URL resourceUrl = CarAnalyzer.class.getResource("/");
        return resourceUrl.getPath() + "/" + testName + "/" + DEFAULT_ESBDOC_RAW_PATH;
    }

    private File createCarFile(String testName) throws Exception {
        URL resourceUrl = CarAnalyzer.class.getResource("/" + testName);

        String sourceFolder = resourceUrl.getPath();
        String targetArchive = sourceFolder + "/" + testName + ".car";

        Zipper.zipFolder(sourceFolder, targetArchive);

        return new File(targetArchive);
    }

}
