package fi.mystes.esbdoc;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.*;

import java.io.*;
import java.net.URL;
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

        createCarFile("testWithSingleProxy");

        //File[] carFiles = org.apache.commons.compress.compressors.gzip.GzipUtils.
        String esbdocRawPath = DEFAULT_ESBDOC_RAW_PATH;
        File[] soapUiFileSet = new File[0];
        //car.run(carFiles, esbdocRawPath, soapUiFileSet);
    }

    private void createCarFile(String testName) throws Exception {
        URL resourceUrl = CarAnalyzer.class.getResource("/" + testName);

        String sourceFolder = resourceUrl.getPath();
        String targetArchive = sourceFolder + "/" + testName + ".car";
        zipFolder(sourceFolder, targetArchive);
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
            logicalPath = StringUtils.equals(logicalPath, "") ? folder.getName() : logicalPath + "/" + folder.getName();
            addToZip(logicalPath, physicalPath + "/" + fileName, zip);
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
