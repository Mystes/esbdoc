package fi.mystes.esbdoc;

import java.io.File;
import java.net.URL;

/**
 * Created by mystes-am on 5.4.2016.
 */
public class CarFileUtil {

    public static File createCarFile(String testName) throws Exception {
        URL resourceUrl = CarAnalyzer.class.getResource("/" + testName);

        String sourceFolder = resourceUrl.getPath();
        String targetArchive = sourceFolder + "/" + testName + ".car";

        Zipper.zipFolder(sourceFolder, targetArchive);

        return new File(targetArchive);
    }

    public static File createCarFile(String testName, String carName) throws Exception {
        URL testResourcesUrl = CarAnalyzer.class.getResource("/" + testName);
        URL carResourcesUrl = CarAnalyzer.class.getResource("/" + testName + "/" + carName);

        String sourceFolder = carResourcesUrl.getPath();
        String targetFolder = testResourcesUrl.getPath();

        String targetArchive = targetFolder + "/" + carName + ".car";

        Zipper.zipFolder(sourceFolder, targetArchive);

        return new File(targetArchive);
    }

    public static File getTestFolder(String testName, String soapUiFolderName) {
        URL testFolderUrl = CarAnalyzer.class.getResource("/" + testName + "/" + soapUiFolderName);
        String testFolderPath = testFolderUrl.getPath();
        return new File(testFolderPath);
    }
}
