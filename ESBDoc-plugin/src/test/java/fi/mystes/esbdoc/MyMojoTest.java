package fi.mystes.esbdoc;

import net.sf.saxon.s9api.SaxonApiException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.model.FileSet;
import org.jaxen.JaxenException;
import org.junit.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by mystes-am on 9.6.2016.
 */
public class MyMojoTest {

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
    public void testArtifactNameWithNoDot() throws Exception {
        String testName = "MyMojoTest_testArtifactNameWithNoDot";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination("MyMojoTest_", "..");
        String targetPath = esbDocRawPath + "target/";

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInProxyName() throws Exception {
        String testName = "MyMojoTest_testArtifactNameWithDotInProxyName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination("MyMojoTest_", "..");
        String targetPath = esbDocRawPath + "target/";

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInDeploymentFolderName() throws Exception {
        String testName = "MyMojoTest_testArtifactNameWithDotInDeploymentFolderName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment.Dot");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination("MyMojoTest_", "..");
        String targetPath = esbDocRawPath + "target/";

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInSoapUiFolderName() throws Exception {
        String testName = "MyMojoTest_testArtifactNameWithDotInSoapUiFolderName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment.Dot");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination("MyMojoTest_", "..");
        String targetPath = esbDocRawPath + "target/";

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInSoapUiFileName() throws Exception {
        String testName = "MyMojoTest_testArtifactNameWithDotInSoapUiFileName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment.Dot");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination("MyMojoTest_", "..");
        String targetPath = esbDocRawPath + "target/";

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInOutputFolderName() throws Exception {
        String testName = "MyMojoTest_testArtifactNameWithDotInOutputFolderName.Dot";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment.Dot");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        URL resourceUrl = CarAnalyzer.class.getResource("/");
        String targetPath = resourceUrl.getPath() + "MyMojoTest_testArtifactNameWithDotInOutputFolderName.Dot/" + "target/";

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInProxyFolderName() throws Exception {
        String testName = "MyMojoTest_testArtifactNameWithDotInProxyFolderName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination("MyMojoTest_", "..");
        String targetPath = esbDocRawPath + "target/";

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    /***********************************************************************************************/

    private static class MojoGenerator {
        private MyMojo mojo;

        private MojoGenerator(){
            this.mojo = new MyMojo();
        }

        private MyMojo getMojo(){
            return mojo;
        }

        public static MyMojo gimmeMojo(String targetPath, File[] carFiles) throws NoSuchFieldException, IllegalAccessException {
            FileSet soapUIFileSet = new FileSet();
            soapUIFileSet.setDirectory(null);
            soapUIFileSet.setExcludes(new ArrayList<String>());
            List<String> includes = new ArrayList<String>();
            includes.add("*.xml");
            soapUIFileSet.setIncludes(includes);
            return gimmeMojo(targetPath, carFiles, soapUIFileSet);
        }

        public static MyMojo gimmeMojo(String targetPath, File[] carFiles, FileSet soapUIFileSet) throws NoSuchFieldException, IllegalAccessException {
            File target = new File(targetPath);
            return gimmeMojo(target, carFiles, soapUIFileSet);
        }

        public static MyMojo gimmeMojo(File target, File[] carFiles, FileSet soapUIFileSet) throws NoSuchFieldException, IllegalAccessException {
            File projectParentdDir = target.getParentFile();
            return gimmeMojo(null, target, carFiles, soapUIFileSet, projectParentdDir);
        }

        public static MyMojo gimmeMojo(String esbdocRawFilename, File target, File[] carFiles, FileSet soapUIFileSet, File projectParentdDir) throws NoSuchFieldException, IllegalAccessException {
            MojoGenerator moge = new MojoGenerator();
            moge.setField("esbdocRawFilename", esbdocRawFilename);
            moge.setField("target", target);
            moge.setField("carFiles", carFiles);
            moge.setField("soapUIFileSet", soapUIFileSet);
            moge.setField("projectParentdDir", projectParentdDir);

            return moge.getMojo();
        }

        private <T extends Object> void setField(String fieldName, T value) throws NoSuchFieldException, IllegalAccessException {
            Field field = MyMojo.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(mojo, value);
        }
    }

    /***********************************************************************************************/

    private class CarAnalyzerSelfShunt extends CarAnalyzer implements SelfShunt {
        private File[] expectedCarFiles;
        private String expectedDestination;

        private File[] actualCarFiles;
        private String actualDestination;

        private boolean called = false;

        public CarAnalyzerSelfShunt(File[] carFileArray, String targetPath) throws FileSystemException, ParserConfigurationException, JaxenException {
            this.expectedCarFiles = carFileArray;
            this.expectedDestination = targetPath;
        }

        @Override
        public void run(boolean validateArtifacts, File[] carFiles, String outputDestination, File[] testFolders) throws IOException, SaxonApiException, ParserConfigurationException, SAXException, XPathExpressionException, JaxenException {
            this.called = true;
            this.actualCarFiles = carFiles;
            this.actualDestination = outputDestination;
        }

        public void assertStatus(){
            assertTrue("CarAnalyzer should be called by MyMojo.", this.called);
            assertEquals("Car Files ordered for processing by MyMojo should cover all given Car files.", this.expectedCarFiles, this.actualCarFiles);
            assertEquals("Output destination declared by MyMojo should match expected output destination.", this.expectedDestination, this.actualDestination);
        }
    }

    private interface SelfShunt {
        void assertStatus();
    }

    /***********************************************************************************************/


    private static String getMethodNameOf(String path){
        if(StringUtils.equals(path, ".")){
            return Thread.currentThread().getStackTrace()[2 + 0].getMethodName();
        }
        if(StringUtils.equals(path, "..")){
            return Thread.currentThread().getStackTrace()[2 + 1].getMethodName();
        }
        if(StringUtils.equals(path, "...")){
            return Thread.currentThread().getStackTrace()[2 + 2].getMethodName();
        }
        int depth = StringUtils.split(path, "/").length;
        return Thread.currentThread().getStackTrace()[2 + depth].getMethodName();
    }

    private String outputDestination(String testNamePrefix, String depthPath){
        String testName = getMethodNameOf(depthPath);
        URL resourceUrl = CarAnalyzer.class.getResource("/");
        return resourceUrl.getPath() + testNamePrefix + testName + "/";
    }

}
