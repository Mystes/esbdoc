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

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;

import fi.mystes.esbdoc.CarAnalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Goal which touches a timestamp file.
 *
 * @goal generate
 */
public class MyMojo
    extends AbstractMojo
{
    /**
     * Location of the file.
     * @parameter
     * @required
     */
    private File outputFileDestination;

    /**
     * @parameter
     */
    private File[] carFiles;

    /**
     * @parameter
     */
    private File[] soapUIFolders;

    public void execute() throws MojoExecutionException {
        try {
            CarAnalyzer car = new CarAnalyzer();
            car.run(carFiles, outputFileDestination.getAbsolutePath(), soapUIFolders);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
