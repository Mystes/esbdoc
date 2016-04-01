/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fi.mystes.esbdoc;

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
        payload = source;
        if (!source.isEmpty()) {
            String[] lines = source.split("\n");
            if (lines[0].contains("Title ")) {
                name = lines[0].substring(6);
            }
            if (lines.length > 1) {
//                System.out.println("Parsing:"+name);
                int i = 0;
                for (String l : lines) {
//                    System.out.println((i++) + " " + l);
                    if (l.contains("->")) {
                        int ind = l.indexOf("->");
                        // Remove also ':'-from the line
                        String target = l.substring(ind + 4, l.length()-1);
                        // Add targets to the list.
                        leaves.add(target);
                    }
                }
            }
        }
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
