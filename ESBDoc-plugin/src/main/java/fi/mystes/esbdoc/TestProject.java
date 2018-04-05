/**
 * Copyright 2018 Mystes Oy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */

import java.util.Set;

/**
 * Represents a single TestProject
 */
public class TestProject implements Comparable<TestProject> {

    private final String name;
    private final String filename;
    private final Set<TestSuite> suites;

    public TestProject(String name, String filename, Set<TestSuite> suites) {
        assertNotNull("name", name);
        assertNotNull("filename", filename);
        assertNotNull("suites", suites);

        this.name = name;
        this.filename = filename;
        this.suites = suites;
    }

    private void assertNotNull(String paramName, Object param){
        if(null == param){
            throw new IllegalArgumentException("TestProject constructor parameter '" + paramName + "' must be non-null!");
        }
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public Set<TestSuite> getTestSuites() {
        return suites;
    }

    @Override
    public String toString() {
        return "{name: \"" + name + "\"}";
    }

    @Override
    public int compareTo(TestProject that) {
        return this.name.compareTo(that.name);
    }

    @Override
    public boolean equals(Object object) {
        if(null == object){
            return false;
        }
        if(!(object instanceof TestProject)){
            return false;
        }
        TestProject that = (TestProject) object;
        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return 37 * 37 + name.hashCode(); //TODO why like this?
    }
}