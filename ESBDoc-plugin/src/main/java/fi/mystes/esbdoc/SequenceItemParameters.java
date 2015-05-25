package fi.mystes.esbdoc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mystes-am on 25.5.2015.
 */

public class SequenceItemParameters {
    private final StringBuilder stringBuilder = new StringBuilder();
    private final List<String> handledNodeList = new ArrayList();

    private String key;
    private String parent = null;

    public SequenceItemParameters(String key){
        this.key = key;
    }

    private StringBuilder getStringBuilder(){
        return this.stringBuilder;
    }

    private List<String> getHandledNodeList(){
        return this.handledNodeList;
    }

    public String getKey(){
        return this.key;
    }

    public void setKey(String key){
        this.key = key;
    }

    public String getParent(){
        return this.parent;
    }

    public void setParent(String parent){
        this.parent = parent;
    }

    public String toString(){
        return this.getStringBuilder().toString();
    }

    public boolean containsCircularDependencies(){
        return this.getHandledNodeList().contains(this.getKey());
    }

    public void addDependency(String from, String to){
        this.getStringBuilder().append(dependency(from, to));
    }

    private String dependency(String from, String to){
        return from + "->" + to + ":\n";
    }

    public void addHandledNode(String key){
        this.getHandledNodeList().add(key);
    }

}
