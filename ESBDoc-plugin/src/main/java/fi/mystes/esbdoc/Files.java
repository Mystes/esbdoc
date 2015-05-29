package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static fi.mystes.esbdoc.Constants.FILE_SEPARATOR;

/**
 * Created by mystes-am on 29.5.2015.
 */
public class Files {
    private static Log log = LogFactory.getLog(Files.class);

    public static File[] convertToFileHandles(String commaSeparatedListOfFilenames){
        String[] filenames = StringUtils.split(commaSeparatedListOfFilenames, FILE_SEPARATOR);

        List<File> fileList = new ArrayList<File>();
        for(String filename : filenames){
            fileList.add(new File(filename));
        }

        return fileList.toArray(new File[]{});
    }

    public static List<FileObject> getCarFileObjects(File[] carFiles) throws FileSystemException {
        List<FileObject> carFileObjects = new ArrayList<FileObject>(carFiles.length);

        for (File carFile : carFiles) {
            carFileObjects.add(getCarFileObject(carFile.getAbsolutePath()));
        }

        return carFileObjects;
    }

    private static FileObject getCarFileObject(String carFile) throws FileSystemException {
        File file = new File(carFile);
        if (file.exists()) {
            return VFS.getManager().resolveFile("zip:" + file.getAbsolutePath());
        }
        log.warn(MessageFormat.format("The specified car file [{0}] does not exist.", carFile));
        return null;
    }

    public static List<FileObject> getTestFileObjects(File[] testFiles) throws FileSystemException {
        List<FileObject> testFileObjects = new ArrayList<FileObject>(testFiles.length);

        for (File testFile : testFiles) {
            testFileObjects.add(getTestFileObject(testFile.getAbsolutePath()));
        }

        return testFileObjects;
    }

    private static FileObject getTestFileObject(String testFile) throws FileSystemException {
        File file = new File(testFile);
        if (file.exists()) {
            return VFS.getManager().resolveFile(file.getAbsolutePath());
        }
        log.warn(MessageFormat.format("The specified test file [{0}] does not exist.", testFile));
        return null;
    }
}
