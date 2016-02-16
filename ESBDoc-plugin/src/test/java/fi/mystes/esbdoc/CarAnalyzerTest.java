package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;
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
        String esbDocRawPath = outputDestination();
        car.run(carFiles(), esbDocRawPath, new File[0]);

        assertSequenceModelProxies(esbDocRawPath, "Proxy");

        String esbDocMainModelPath = mainModelPathFor(esbDocRawPath);
        MainModelAssertion mainModelAssertion = new MainModelAssertion(esbDocMainModelPath);
        mainModelAssertion.assertNoTests();
        mainModelAssertion.assertNoDependencies();

        ProxyAssertion proxyAssertion = mainModelAssertion.proxyAssertionFor("Proxy");
        proxyAssertion.assertPurpose("Test ESBDoc with a single proxy");
    }

    public static String getMethodNameOf(String path){
        if(StringUtils.equals(path, ".")){
            return Thread.currentThread().getStackTrace()[2 + 0].getMethodName();
        }
        if(StringUtils.equals(path, "..")){
            return Thread.currentThread().getStackTrace()[2 + 1].getMethodName();
        }
        int depth = StringUtils.split(path, "/").length;
        return Thread.currentThread().getStackTrace()[2 + depth].getMethodName();
    }

    private void assertSequenceModelProxies(String esbDocRawPath, String... proxyNames) throws IOException{
        String esbDocSequencePath = sequencePathFor(esbDocRawPath);
        SequenceModelAssertion sequenceModelAssertion = new SequenceModelAssertion(esbDocSequencePath);

        sequenceModelAssertion.assertSize(proxyNames.length);
        for(String proxyName : proxyNames){
            sequenceModelAssertion.assertContains(proxyName);
        }
    }

    private File[] carFiles() throws Exception{
        List<File> carFileList = new ArrayList<File>();
        carFileList.add(createCarFile(getMethodNameOf("..")));
        return carFileList.toArray(new File[carFileList.size()]);
    }

    private String sequencePathFor(String esbdocRawPath){
        return esbdocRawPath + "-seq.json";
    }

    private String mainModelPathFor(String esbdocRawPath){
        return esbdocRawPath + ".json";
    }

    private String outputDestination(){
        String testName = getMethodNameOf("..");
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
