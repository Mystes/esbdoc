package fi.mystes.esbdoc;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;

import java.io.*;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fi.mystes.esbdoc.Constants.FILE_SEPARATOR;

/**
 * Created by mystes-am on 29.5.2015.
 */
public class Files {
    private static Log log = LogFactory.getLog(Files.class);

    private enum Type {
        CAR_FILE, SOAPUI_FILE;
    }

    public static File[] convertToFileHandles(String commaSeparatedListOfFilenames){
        String[] filenames = StringUtils.split(commaSeparatedListOfFilenames, FILE_SEPARATOR);

        List<File> fileList = new ArrayList<File>();
        for(String filename : filenames){
            fileList.add(new File(filename));
        }

        return fileList.toArray(new File[]{});
    }

    public static List<FileObject> getCarFileObjects(File[] files) throws FileSystemException {
        return getFileObjects(files, Type.CAR_FILE);
    }

    public static List<FileObject> getTestFileObjects(File[] filesAndFolders) throws FileSystemException {
        List<File> files = new ArrayList<File>();
        List<File> folders = new ArrayList<File>();

        String infoString = "Looking for test files from the following files and folders: ";
        for(File fileOrFolder : filesAndFolders){
            if(fileOrFolder.isDirectory()){
                folders.add(fileOrFolder);
                infoString += "\n" + fileOrFolder.getAbsolutePath() + " (folder)";
            } else {
                files.add(fileOrFolder);
                infoString += "\n" + fileOrFolder.getAbsolutePath() + " (file)";
            }
        }
        log.info(infoString);

        for(File folder : folders){
            File[] filesInFolder = folder.listFiles();
            files.addAll(Arrays.asList(filesInFolder));
        }
        return getFileObjects(files, Type.SOAPUI_FILE);
    }

    private static List<FileObject> getFileObjects(List<File> files, Type type) throws FileSystemException {
        File[] fileArray = files.toArray(new File[files.size()]);
        return getFileObjects(fileArray, type);
    }

    private static List<FileObject> getFileObjects(File[] files, Type type) throws FileSystemException {
        List<FileObject> fileObjects = new ArrayList<FileObject>(files.length);
        for (File file : files) {
            fileObjects.add(getFileObject(file.getAbsolutePath(), type));
        }
        return fileObjects;
    }

    private static FileObject getFileObject(String filename, Type type) throws FileSystemException {
        File file = new File(filename);
        if (file.exists()) {
            return getFileObject(file, type);
        }
        log.warn(MessageFormat.format("The specified file [{0}] does not exist.", filename));
        return null;
    }

    private static FileObject getFileObject(File file, Type type) throws FileSystemException {
        switch (type){
            case CAR_FILE: return resolveZipFile(file);
            default: return resolveNormalFile(file);
        }
    }

    private static FileObject resolveZipFile(File file) throws FileSystemException {
        return VFS.getManager().resolveFile("zip:" + file.getAbsolutePath());
    }

    private static FileObject resolveNormalFile(File file) throws FileSystemException {
        return VFS.getManager().resolveFile(file.getAbsolutePath());
    }

    public static boolean buildDirectoryPathFor(String filename){
        log.info("Building directory path for: " + filename);
        new File(filename).getParentFile().mkdirs();
        return new File(filename).mkdir();
    }

    private static FileOutputStream textOutputFor(String filename) throws FileNotFoundException{
        return outputStreamFor(filename);
    }

    //TODO should be private
    public static FileOutputStream jsonOutputFor(String filename) throws FileNotFoundException{
        return outputStreamFor(filename);
    }

    private static FileOutputStream outputStreamFor(String filename) throws FileNotFoundException{
        return new FileOutputStream(new File(filename));
    }

    private static OutputStreamWriter utf8WriterFor(OutputStream stream){
        return new OutputStreamWriter(stream, Charset.forName("UTF-8"));
    }

    public static void writeTextTo(String filename, List<String> values) throws IOException {
        log.info("Writing textual dependency representation to: " + filename);
        FileOutputStream textStream = textOutputFor(filename);
        writeTo(textStream, values);
        textStream.close();
    }

    public static void writeJsonTo(String filename, List<String> values) throws IOException {
        FileOutputStream jsonStream = jsonOutputFor(filename);
        writeTo(jsonStream, values);
        jsonStream.close();
    }

    private static void writeTo(OutputStream outputStream, List<String> values) throws IOException {
        OutputStreamWriter outputStreamWriter = utf8WriterFor(outputStream);
        writeTo(outputStreamWriter, values);
        outputStreamWriter.close();
    }

    private static void writeTo(OutputStreamWriter outputStreamWriter, List<String> values) throws IOException {
        for (String value : values) {
            outputStreamWriter.write(value);
            outputStreamWriter.write('\n');
        }
    }
}
