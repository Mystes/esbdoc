package fi.mystes.esbdoc;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
}
