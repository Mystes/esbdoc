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

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

/**
 *
 * @author kimmol
 */
class SequenceItem {
    private String name = null;
    private String payload = null;
    private ArrayList<String> leaves = new ArrayList();

    SequenceItem(String source) {
        if(StringUtils.isEmpty(source)){
            return;
        }

        payload = source;

        //TODO what the hell?
        String[] lines = StringUtils.split(source, "\n");
        if (lines[0].contains("Title ")) {
            name = lines[0].substring(6); //TODO why 6?
        }

        if(lines.length <= 1){ //TODO why like this?
            return;
        }

        for (String line : lines) {
            if (isTargetDefined(line)) {
                addTargetAsNewLeaf(line);
            }
        }
    }

    private boolean isTargetDefined(String line){
        return StringUtils.contains(line, "->");
    }

    private void addTargetAsNewLeaf(String line){
        String target = findTarget(line);
        leaves.add(target);
    }

    private String findTarget(String line){
        int indexOfArrow = line.indexOf("->");
        return removeColonFromLine(line, indexOfArrow);
    }

    private String removeColonFromLine(String line, int indexOfArrow) {
        return line.substring(indexOfArrow + 4, line.length()-1);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<String> getLeaves() {
        return leaves;
    }
    
}
