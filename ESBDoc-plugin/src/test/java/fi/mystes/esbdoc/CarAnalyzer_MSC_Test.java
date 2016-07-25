package fi.mystes.esbdoc;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.*;

/**
 * Created by mystes-am on 05/04/16.
 */
public class CarAnalyzer_MSC_Test {

    /***********************************************************************************************/

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {

    }

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

    }

    /***********************************************************************************************/

    @Test
    public void testWithSingleProxy() throws Exception {
        String esbDocRawPath = outputDestination();
        new CarAnalyzer().run(false, carFiles(), esbDocRawPath, new File[0]);

        assertSequenceModelContains(esbDocRawPath, "Proxy");
    }

    @Test
    public void testWithTwoIndependentProxies() throws Exception {
        String esbDocRawPath = outputDestination();
        new CarAnalyzer().run(false, carFiles(), esbDocRawPath, new File[0]);

        assertSequenceModelContains(esbDocRawPath, "Proxy1", "Proxy2");
    }

    @Test
    public void testWithOneProxyAndOneSequence() throws Exception {
        String esbDocRawPath = outputDestination();
        new CarAnalyzer().run(false, carFiles(), esbDocRawPath, new File[0]);

        assertSequenceModelContains(esbDocRawPath, "ProxyWithOneSequence", "TheSequence");
    }

    @Test
    public void testWithOneProxyAndTwoSequences() throws Exception {
        String esbDocRawPath = outputDestination();
        new CarAnalyzer().run(true, carFiles(), esbDocRawPath, new File[0]);

        assertSequenceModelContains(esbDocRawPath, "ProxyWithTwoSequences", "SequenceOne", "SequenceTwo");
    }

    @Test(expected = EsbDocException.class)
    public void testWithOneProxyAndTwoSequencesWithoutDescription() throws Exception {
        String esbDocRawPath = outputDestination();
        new CarAnalyzer().run(true, carFiles(), esbDocRawPath, new File[0]);
    }


    @Test
    public void testWithOneProxyReferencingOneSequenceTwice() throws Exception {
        String esbDocRawPath = outputDestination();
        new CarAnalyzer().run(false, carFiles(), esbDocRawPath, new File[0]);

        //TODO perhaps this should contain the sequence reference twice since it is called twice? Or maybe not. Think about this later.
        assertSequenceModelContains(esbDocRawPath, "ProxyReferencingOneSequenceTwice", "TheSequence");
    }

    @Test
    public void testWithTwoIndependentProxiesReferencingTwoSequencesEach() throws Exception {
        String esbDocRawPath = outputDestination();
        new CarAnalyzer().run(false, carFiles(), esbDocRawPath, new File[0]);

        assertSequenceModelContains(esbDocRawPath, "Proxy1", "Proxy2", "SequenceOne", "SequenceTwo");
    }

    /***********************************************************************************************/

    private void assertSequenceModelContains(String esbDocRawPath, String... items) throws IOException{
        String esbDocSequencePath = sequencePathFor(esbDocRawPath);
        SequenceModelAssertion sequenceModelAssertion = new SequenceModelAssertion(esbDocSequencePath);

        sequenceModelAssertion.assertSize(items.length);
        for(String item : items){
            sequenceModelAssertion.assertContains(item);
        }
    }

    /***********************************************************************************************/

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

    private File[] carFiles() throws Exception{
        List<File> carFileList = new ArrayList<File>();
        carFileList.add(CarFileUtil.createCarFile(getMethodNameOf("..")));
        return carFileList.toArray(new File[carFileList.size()]);
    }

    private String outputDestination(){
        String testName = getMethodNameOf("..");
        URL resourceUrl = CarAnalyzer.class.getResource("/");
        return resourceUrl.getPath() + testName + "/";
    }

    private String sequencePathFor(String basePath){
        return basePath + Constants.MSC_JSON_FILE;
    }


}
