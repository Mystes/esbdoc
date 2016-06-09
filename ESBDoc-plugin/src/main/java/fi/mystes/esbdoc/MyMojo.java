package fi.mystes.esbdoc;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import org.apache.commons.vfs2.*;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.jaxen.JaxenException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

//TODO Take a look at this class later
/**
 * Goal which touches a timestamp file.
 *
 * @goal generate
 */
public class MyMojo
        extends AbstractMojo {

    private CarAnalyzer carAnalyzer = null;

    /**
     * Name of the file where esbdoc raw data is generated (both json and txt)
     *
     * @parameter default-value="esbdoc-raw"
     * @required
     */
    private String esbdocRawFilename;

    /**
     * Target folder where to generate esbdoc
     *
     * @parameter default-value="${project.build.directory}"
     */
    private File target;

    /**
     * List of car files to process
     *
     * @parameter
     * @required
     */
    private File[] carFiles;

    /**
     * {@link org.apache.maven.model.FileSet} SoapUI test project files given as
     * fileset
     *
     *
     * @parameter
     * @required
     * @since 1.0
     */
    private FileSet soapUIFileSet;

    /**
     * @parameter default-value="${project.parent.basedir}"
     */
    private File projectParentdDir;

    private FileSystemManager fsm;

    public void execute() throws MojoExecutionException {
        try {
            fsm = VFS.getManager();
            // targets for esbdoc source
            String esbdocRawPath = target.getAbsolutePath() + "/";
 
            // analyze car file and test files, and build esbdoc
            CarAnalyzer car = getCarAnalyzer();
            car.run(carFiles, esbdocRawPath, getSoapUIFileSet());
            
            // copy UI to target folder
            URL url = this.getClass().getClassLoader().getResource("UI");
            FileObject uiTargetFolder = fsm.resolveFile(target.getAbsolutePath() + "/UI");
            uiTargetFolder.copyFrom(fsm.resolveFile("zip:" + url.getPath()), Selectors.SELECT_ALL);

            // replace UI index.html file with generated esbdoc data
            //TODO File name should be dynamic even if default comes from Constants
            String json = FileUtils.fileRead(new File(esbdocRawPath + Constants.PHYSICAL_DEPENDENCY_JSON_FILE));
            String indexContent = FileUtils.fileRead(new File(uiTargetFolder.getChild("index.html").getURL().getPath()));
            String regexToFindPlaceholder = "window.ESBDOCDATA\\s*=\\s*.[^;]*;";
            indexContent = indexContent.replaceAll(regexToFindPlaceholder, "window.ESBDOCDATA=" + escapeCharsFromJson(json) + ";");
            FileUtils.fileWrite(uiTargetFolder.getChild("index.html").getURL().getPath(), indexContent);

            System.out.println("\nTo view ESBDoc open your browser and put this link to the address field:\n"
                    + "(Tip! In Mac OS X CMD+Double click)\n"
                    + "file://" + uiTargetFolder.getURL().getPath() + "/index.html\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escapeCharsFromJson(String json) {
        return json.replaceAll("\\$", "\\\\\\$");
    }

    /**
     * @param list
     * @return
     */
    protected String getCommaSeparatedList(List list) {
        StringBuilder buffer = new StringBuilder();
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            Object element = iterator.next();
            buffer.append(element.toString());
            if (iterator.hasNext()) {
                buffer.append(",");
            }
        }
        return buffer.toString();
    }

    /**
     * @return @throws MojoExecutionException
     */
    protected File[] getSoapUIFileSet() throws MojoExecutionException {
        try {
            String dir=soapUIFileSet.getDirectory();
            // No absolute directory set, use parent basedir as a default
            if(dir==null){
                dir = this.projectParentdDir.getAbsolutePath();
            }
            
            File directory = new File(dir);
            String includes = getCommaSeparatedList(this.soapUIFileSet.getIncludes());
            String excludes = getCommaSeparatedList(this.soapUIFileSet.getExcludes());
            List files = FileUtils.getFiles(directory, includes, excludes, true);            
            return (File[]) files.toArray(new File[files.size()]);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to get paths from soapUIFileSet()", e);
        }
    }

    protected CarAnalyzer getCarAnalyzer() throws FileSystemException, ParserConfigurationException, JaxenException {
        if(null == this.carAnalyzer){
            setCarAnalyzer(new CarAnalyzer());
        }
        return this.carAnalyzer;
    }

    protected void setCarAnalyzer(CarAnalyzer carAnalyzer) {
        this.carAnalyzer = carAnalyzer;
    }
}
