package fi.mystes.esbdoc;

import net.sf.saxon.s9api.SaxonApiException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.*;
import org.apache.commons.vfs2.provider.zip.ZipFileObject;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import static org.junit.Assert.*;

/**
 * Created by mystes-am on 9.6.2016.
 */
public class MyMojoTest {

    private         Path sourcePath;
    public static final String PREFIX = "";

    /***********************************************************************************************/

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        FileSystemManager fsm;
        // zip UI-directory and copy it to correct place
        fsm = VFS.getManager();
        // copy UI to target zip
        URL url = this.getClass().getClassLoader().getResource("UI");
        sourcePath =  Paths.get(url.getPath());
        Path targetPath =  Paths.get(url.getPath()+".zip");

        String parent=sourcePath.toFile().getParent();

        File directoryToZip = sourcePath.toFile();

        List<File> fileList = new ArrayList<File>();
        getAllFiles(directoryToZip, fileList);
        writeZipFile(targetPath.toFile(),directoryToZip, fileList);
        // Rename odiginal as backup and change zip name
        Paths.get(parent+"/UI").toFile().renameTo(Paths.get(parent+"/UI_DIR").toFile());
        Paths.get(parent+"/UI.zip").toFile().renameTo(Paths.get(parent+"/UI").toFile());
    }

    public static void getAllFiles(File dir, List<File> fileList) {
            File[] files = dir.listFiles();
            for (File file : files) {
                fileList.add(file);
                if (file.isDirectory()) {
                    getAllFiles(file, fileList);
                }
            }
    }

    public static void writeZipFile(File targetZip, File directoryToZip, List<File> fileList) {

        try {
            FileOutputStream fos = new FileOutputStream(targetZip.getAbsoluteFile() );
            ZipOutputStream zos = new ZipOutputStream(fos);

            for (File file : fileList) {
                if (!file.isDirectory()) { // we only zip files, not directories
                    addToZip(directoryToZip, file, zos);
                }
            }

            zos.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void addToZip(File directoryToZip, File file, ZipOutputStream zos) throws FileNotFoundException,
            IOException {

        FileInputStream fis = new FileInputStream(file);

        // we want the zipEntry's path to be a relative path that is relative
        // to the directory being zipped, so chop off the rest of the path
        String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1,
                file.getCanonicalPath().length());
        ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }

        zos.closeEntry();
        fis.close();
    }

    @After
    public void tearDown() throws Exception {
        String parent=sourcePath.toFile().getParent();
        // Remove zip
        Paths.get(parent+"/UI").toFile().delete();
        // Rename backup back to original
        Paths.get(parent+"/UI_DIR").toFile().renameTo(Paths.get(parent+"/UI").toFile());
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {

    }

    /***********************************************************************************************/

    @Test
    public void testArtifactNameWithNoDot() throws Exception {
        String testName = PREFIX + "testArtifactNameWithNoDot";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination(PREFIX, "..");
        String targetPath = esbDocRawPath;

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInProxyName() throws Exception {
        String testName = PREFIX + "testArtifactNameWithDotInProxyName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination(PREFIX, "..");
        String targetPath = esbDocRawPath;

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInDeploymentFolderName() throws Exception {
        String testName = PREFIX + "testArtifactNameWithDotInDeploymentFolderName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment.Dot");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination(PREFIX, "..");
        String targetPath = esbDocRawPath;

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInSoapUiFolderName() throws Exception {
        String testName = PREFIX + "testArtifactNameWithDotInSoapUiFolderName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment.Dot");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination(PREFIX, "..");
        String targetPath = esbDocRawPath;

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInSoapUiFileName() throws Exception {
        String testName = PREFIX + "testArtifactNameWithDotInSoapUiFileName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment.Dot");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination(PREFIX, "..");
        String targetPath = esbDocRawPath;

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInOutputFolderName() throws Exception {
        String testName = PREFIX + "testArtifactNameWithDotInOutputFolderName.Dot";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment.Dot");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        URL resourceUrl = CarAnalyzer.class.getResource("/");
        String targetPath = resourceUrl.getPath() + PREFIX + "testArtifactNameWithDotInOutputFolderName.Dot/";

        MyMojo mojo = MojoGenerator.gimmeMojo(targetPath, carFileArray);
        mojo.setCarAnalyzer(new CarAnalyzerSelfShunt(carFileArray, targetPath));
        mojo.execute();
        ((SelfShunt)mojo.getCarAnalyzer()).assertStatus();
    }

    @Test
    public void testArtifactNameWithDotInProxyFolderName() throws Exception {
        String testName = PREFIX + "testArtifactNameWithDotInProxyFolderName";
        File carFile = CarFileUtil.createCarFile(testName, "Deployment");

        File[] carFileArray = ArrayUtils.toArray(carFile);
        String esbDocRawPath = outputDestination(PREFIX, "..");
        String targetPath = esbDocRawPath;

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
            super.run(validateArtifacts, carFiles, outputDestination, testFolders);
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
