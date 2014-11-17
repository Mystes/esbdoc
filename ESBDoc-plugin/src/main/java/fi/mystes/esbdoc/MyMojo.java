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
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.net.URL;

/**
 * Goal which touches a timestamp file.
 *
 * @goal generate
 */
public class MyMojo
    extends AbstractMojo
{
    /**
     * Name of the file where esbdoc raw data is generated (both json and txt)
     * @parameter default-value="esbdoc-raw"
     * @required
     */
    private String esbdocRawFilename;

    /**
     * Target folder where to generate esbdoc
     * @parameter default-value="${project.build.directory}"
     */
    private File target;

    /**
     * List of car files to process
     * @parameter
     * @required
     */
    private File[] carFiles;

    /**
     * List of folders that contain SoapUI test project files
     * @parameter
     * @required
     */
    private File[] soapUIFolders;

    private FileSystemManager fsm;

    public void execute() throws MojoExecutionException {
        try {
            fsm = VFS.getManager();

            // targets for esbdoc source
            String esbdocRawPath = target.getAbsolutePath() + "/" + esbdocRawFilename;

            // analyze car file and test files, and build esbdoc
            CarAnalyzer car = new CarAnalyzer();
            car.run(carFiles, esbdocRawPath , soapUIFolders);

            // copy UI to target folder
            URL url = this.getClass().getClassLoader().getResource("UI");
            FileObject uiTargetFolder = fsm.resolveFile(target.getAbsolutePath() + "/UI");
            uiTargetFolder.copyFrom(fsm.resolveFile("zip:" + url.getPath()), Selectors.SELECT_ALL);

            // replace UI index.html file with generated esbdoc data
            String json = FileUtils.fileRead(new File(esbdocRawPath + ".json"));
            String indexContent = FileUtils.fileRead(new File(uiTargetFolder.getChild("index.html").getURL().getPath()));
            String regexToFindPlaceholder = "window.ESBDOCDATA\\s*=\\s*.[^;]*;";
            indexContent = indexContent.replaceAll(regexToFindPlaceholder,"window.ESBDOCDATA=" + escapeCharsFromJson(json) + ";");
            FileUtils.fileWrite(uiTargetFolder.getChild("index.html").getURL().getPath(), indexContent);

            System.out.println("\nTo view ESBDoc open your browser and put this link to the address field:\n" +
                "(Tip! In Mac OS X CMD+Double click)\n" +
                "file://" + uiTargetFolder.getURL().getPath() + "/index.html\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String escapeCharsFromJson(String json) {
        return json.replaceAll("\\$", "\\\\\\$");
    }
}
