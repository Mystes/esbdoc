package fi.mystes.esbdoc;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by mystes-am on 16.2.2016.
 */
public class Zipper {

    private static List<String> ignoredRootLevelFiles = new ArrayList<String>();
    static {
        ignoredRootLevelFiles.add("esbdoc-raw.txt");
        ignoredRootLevelFiles.add("esbdoc-raw.json");
        ignoredRootLevelFiles.add("esbdoc-raw-seq.json");
    }

    public static void zipFolder(String sourceFolderPath, String targetArchive) throws Exception {
        FileOutputStream fileWriter = new FileOutputStream(targetArchive);
        ZipOutputStream zip = new ZipOutputStream(fileWriter);

        addRootLevelToZip(sourceFolderPath, zip, targetArchive);

        zip.flush();
        zip.close();
    }

    private static void addRootLevelToZip(String physicalPath, ZipOutputStream zip, String targetArchive) throws Exception {

        targetArchive = localArchivePath(physicalPath, targetArchive);
        String[] fileNames = rootFilter(physicalPath, targetArchive);

        for (String fileName : fileNames) {
            addToZip("", physicalPath + "/" + fileName, zip);
        }
    }

    private static String localArchivePath(String physicalPath, String targetArchive){
        targetArchive = StringUtils.remove(targetArchive, physicalPath);
        return targetArchive.startsWith("/") ? StringUtils.remove(targetArchive, "/") : targetArchive;
    }

    private static String[] rootFilter(String physicalPath, String targetArchive){
        String[] fileNames = doNotAddArchiveFileToItself(physicalPath, targetArchive);
        fileNames = doNotAddIgnoredFiles(fileNames);

        return fileNames;
    }

    private static String[] doNotAddArchiveFileToItself(String physicalPath, String targetArchive) {
        File rootFolder = new File(physicalPath);
        String[] fileNames = rootFolder.list();
        fileNames = ArrayUtils.removeElement(fileNames, targetArchive);
        return fileNames;
    }

    private static String[] doNotAddIgnoredFiles(String[] fileNames) {
        for(String ignoredFile : ignoredRootLevelFiles){
            fileNames = ArrayUtils.removeElement(fileNames, ignoredFile);
        }
        return fileNames;
    }

    private static void addFolderToZip(String logicalPath, String physicalPath, ZipOutputStream zip) throws Exception {
        File folder = new File(physicalPath);

        for (String fileName : folder.list()) {
            String currentLogicalPath = StringUtils.equals(logicalPath, "") ? folder.getName() : logicalPath + "/" + folder.getName();
            addToZip(currentLogicalPath, physicalPath + "/" + fileName, zip);
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
