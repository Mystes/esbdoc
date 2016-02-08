package fi.mystes.esbdoc;

import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.jaxen.JaxenException;
import org.junit.*;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

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

        //TODO File[] carFiles =
        String esbdocRawPath = DEFAULT_ESBDOC_RAW_PATH;
        File[] soapUiFileSet = new File[0];
        //car.run(carFiles, esbdocRawPath, soapUiFileSet);
    }
}
