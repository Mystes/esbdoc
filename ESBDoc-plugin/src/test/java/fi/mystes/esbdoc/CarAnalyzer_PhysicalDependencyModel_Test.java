package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileSystemException;
import org.jaxen.JaxenException;
import org.junit.*;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.util.*;

import static fi.mystes.esbdoc.DependencyAssertion.EndpointModel.*;

/**
 * Created by jussi on 05/02/16.
 */
public class CarAnalyzer_PhysicalDependencyModel_Test {

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
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.assertNoDependencies();

        mainModel.proxyAssertionFor("Proxy").assertPurpose("Test ESBDoc with a single proxy");
    }

    @Test
    public void testWithTwoIndependentProxies() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.assertNoDependencies();

        mainModel.proxyAssertionFor("Proxy1").assertPurpose("Test ESBDoc with two proxies: Proxy 1");
        mainModel.proxyAssertionFor("Proxy2").assertPurpose("Test ESBDoc with two proxies: Proxy 2");
    }

    @Test
    public void testWithOneProxyAndOneSequence() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.dependencyAssertionFor("ProxyWithOneSequence").forwardsTo(EXCLUSIVELY, "TheSequence");
        mainModel.dependencyAssertionFor("ProxyWithOneSequence").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("TheSequence").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("TheSequence").reversesTo(NON_EXCLUSIVELY, "ProxyWithOneSequence");

        mainModel.proxyAssertionFor("ProxyWithOneSequence").assertPurpose("Test ESBDoc with one proxy and one sequence: The proxy");
        mainModel.sequenceAssertionFor("TheSequence").assertPurpose("Test ESBDoc with one proxy and one sequence: The sequence");
    }

    @Test
    public void testWithOneProxyAndTwoSequences() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.dependencyAssertionFor("ProxyWithTwoSequences").forwardsTo(EXCLUSIVELY, "SequenceOne", "SequenceTwo");
        mainModel.dependencyAssertionFor("ProxyWithTwoSequences").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("SequenceOne").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("SequenceOne").reversesTo(NON_EXCLUSIVELY, "ProxyWithTwoSequences");

        mainModel.dependencyAssertionFor("SequenceTwo").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("SequenceTwo").reversesTo(NON_EXCLUSIVELY, "ProxyWithTwoSequences");

        mainModel.proxyAssertionFor("ProxyWithTwoSequences").assertPurpose("Test ESBDoc with one proxy and two sequences: The proxy");
        mainModel.sequenceAssertionFor("SequenceOne").assertPurpose("Test ESBDoc with one proxy and two sequences: The first sequence");
        mainModel.sequenceAssertionFor("SequenceTwo").assertPurpose("Test ESBDoc with one proxy and two sequences: The second sequence");
    }

    @Test
    public void testWithOneProxyReferencingOneSequenceTwice() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        // This containw only one reference to the sequence since we are dealing with physical dependency from one place to another
        mainModel.dependencyAssertionFor("ProxyReferencingOneSequenceTwice").forwardsTo(EXCLUSIVELY, "TheSequence");
        mainModel.dependencyAssertionFor("ProxyReferencingOneSequenceTwice").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("TheSequence").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("TheSequence").reversesTo(NON_EXCLUSIVELY, "ProxyReferencingOneSequenceTwice");

        mainModel.proxyAssertionFor("ProxyReferencingOneSequenceTwice").assertPurpose("Test ESBDoc with one proxy referencing one sequence twice: The proxy");
        mainModel.sequenceAssertionFor("TheSequence").assertPurpose("Test ESBDoc with one proxy referencing one sequence twice: The sequence");
    }

    @Test
    public void testWithTwoIndependentProxiesReferencingTwoSequencesEach() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.dependencyAssertionFor("Proxy1").forwardsTo(EXCLUSIVELY, "SequenceOne", "SequenceTwo");
        mainModel.dependencyAssertionFor("Proxy1").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("Proxy2").forwardsTo(EXCLUSIVELY, "SequenceOne", "SequenceTwo");
        mainModel.dependencyAssertionFor("Proxy2").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("SequenceOne").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("SequenceOne").reversesTo(NON_EXCLUSIVELY, "Proxy1", "Proxy2");

        mainModel.dependencyAssertionFor("SequenceTwo").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("SequenceTwo").reversesTo(NON_EXCLUSIVELY, "Proxy1", "Proxy2");

        mainModel.proxyAssertionFor("Proxy1").assertPurpose("Test ESBDoc with two independent proxies, each referencing two sequences: Proxy 1");
        mainModel.proxyAssertionFor("Proxy2").assertPurpose("Test ESBDoc with two independent proxies, each referencing two sequences: Proxy 2");
        mainModel.sequenceAssertionFor("SequenceOne").assertPurpose("Test ESBDoc with two independent proxies, each referencing two sequences: The first sequence");
        mainModel.sequenceAssertionFor("SequenceTwo").assertPurpose("Test ESBDoc with two independent proxies, each referencing two sequences: The second sequence");
    }

    @Test
    public void testWithOneProxyReferencingAnotherViaCallout() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.dependencyAssertionFor("Proxy1").forwardsTo(EXCLUSIVELY, "Proxy2");
        mainModel.dependencyAssertionFor("Proxy1").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("Proxy2").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("Proxy2").reversesTo(EXCLUSIVELY, "Proxy1");

        mainModel.proxyAssertionFor("Proxy1").assertPurpose("Test ESBDoc with one proxy referencing another proxy using callout: Proxy 1");
        mainModel.proxyAssertionFor("Proxy2").assertPurpose("Test ESBDoc with one proxy referencing another proxy using callout: Proxy 2");
    }

    @Test
    public void testWithOneProxyReferencingAnotherViaCallUsingAddressEndpoint() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.dependencyAssertionFor("Proxy1").forwardsTo(EXCLUSIVELY, "Proxy2");
        mainModel.dependencyAssertionFor("Proxy1").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("Proxy2").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("Proxy2").reversesTo(EXCLUSIVELY, "Proxy1");

        mainModel.proxyAssertionFor("Proxy1").assertPurpose("Test ESBDoc with one proxy referencing another proxy via call using address endpoint: Proxy 1");
        mainModel.proxyAssertionFor("Proxy2").assertPurpose("Test ESBDoc with one proxy referencing another proxy via call using address endpoint: Proxy 2");
    }

    @Test
    public void testWithOneProxyReferencingAnotherViaSendUsingAddressEndpoint() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.dependencyAssertionFor("Proxy1").forwardsTo(EXCLUSIVELY, "Proxy2");
        mainModel.dependencyAssertionFor("Proxy1").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("Proxy2").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("Proxy2").reversesTo(EXCLUSIVELY, "Proxy1");

        mainModel.proxyAssertionFor("Proxy1").assertPurpose("Test ESBDoc with one proxy referencing another proxy via send using address endpoint: Proxy 1");
        mainModel.proxyAssertionFor("Proxy2").assertPurpose("Test ESBDoc with one proxy referencing another proxy via send using address endpoint: Proxy 2");
    }

    @Test
    public void testWithOneProxyReferencingAnotherViaInlinedAddressEndpoint() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.dependencyAssertionFor("Proxy1").forwardsTo(EXCLUSIVELY, "Proxy2");
        mainModel.dependencyAssertionFor("Proxy1").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("Proxy2").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("Proxy2").reversesTo(EXCLUSIVELY, "Proxy1");

        mainModel.proxyAssertionFor("Proxy1").assertPurpose("Test ESBDoc with one proxy referencing another proxy via inlined address endpoint: Proxy 1");
        mainModel.proxyAssertionFor("Proxy2").assertPurpose("Test ESBDoc with one proxy referencing another proxy via inlined address endpoint: Proxy 2");
    }

    @Test
    public void testWithOneProxyReferencingAnotherViaIndependentAddressEndpoint() throws Exception {
        MainModelAssertion mainModel = mainModelWithNoTests();

        mainModel.dependencyAssertionFor("Proxy1").forwardsTo(EXCLUSIVELY, "AddressEndpointToProxy2");
        mainModel.dependencyAssertionFor("Proxy1").reversesTo(NOWHERE);

        mainModel.dependencyAssertionFor("AddressEndpointToProxy2").forwardsTo(EXCLUSIVELY, "Proxy2");
        mainModel.dependencyAssertionFor("AddressEndpointToProxy2").reversesTo(EXCLUSIVELY, "Proxy1");

        mainModel.dependencyAssertionFor("Proxy2").forwardsTo(NOWHERE);
        mainModel.dependencyAssertionFor("Proxy2").reversesTo(EXCLUSIVELY, "AddressEndpointToProxy2");

        mainModel.proxyAssertionFor("Proxy1").assertPurpose("Test ESBDoc with one proxy referencing another proxy via independent address endpoint: Proxy 1");
        mainModel.proxyAssertionFor("Proxy2").assertPurpose("Test ESBDoc with one proxy referencing another proxy via independent address endpoint: Proxy 2");

        mainModel.endpointAssertionFor("AddressEndpointToProxy2").assertPurpose("Test ESBDoc with one proxy referencing another proxy via independent address endpoint: Address endpoint to Proxy 2");
    }

    /***********************************************************************************************/

    private MainModelAssertion mainModelWithNoTests() throws Exception {
        String esbDocRawPath =  outputDestination("...");

        new CarAnalyzer().run(carFiles("..."), esbDocRawPath, new File[0]);

        String esbDocMainModelPath = mainModelPathFor(esbDocRawPath);
        MainModelAssertion mainModel = new MainModelAssertion(esbDocMainModelPath);
        mainModel.assertNoTests();

        return mainModel;
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

    private File[] carFiles(String depthPath) throws Exception{
        List<File> carFileList = new ArrayList<File>();
        carFileList.add(CarFileUtil.createCarFile(getMethodNameOf(depthPath)));
        return carFileList.toArray(new File[carFileList.size()]);
    }

    private String outputDestination(String depthPath){
        String testName = getMethodNameOf(depthPath);
        URL resourceUrl = CarAnalyzer.class.getResource("/");
        return resourceUrl.getPath() + testName + "/";
    }

    private String mainModelPathFor(String basePath){
        return basePath + Constants.PHYSICAL_DEPENDENCY_JSON_FILE;
    }

}
