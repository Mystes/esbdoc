/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the payload
     */
    public String getPayload() {
        return payload;
    }

    /**
     * @param payload the payload to set
     */
    public void setPayload(String payload) {
        this.payload = payload;
    }

    /**
     * @return the leaves
     */
    public ArrayList<String> getLeaves() {
        return leaves;
    }

    /**
     * @param leaves the leaves to set
     */
    public void setLeaves(ArrayList<String> leaves) {
        this.leaves = leaves;
    }
    
}
