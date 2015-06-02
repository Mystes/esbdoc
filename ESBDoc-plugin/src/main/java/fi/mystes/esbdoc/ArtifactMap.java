package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.TreeMap;

/**
 * Created by mystes-am on 29.5.2015.
 */
public class ArtifactMap extends TreeMap<String, Artifact> {
    public Collection<Artifact> getArtifacts(){
        return this.values();
    }

    public boolean addValid(Artifact artifact){
        if(null == artifact){
            return false;
        }
        if(StringUtils.isBlank(artifact.getName())){
            return false;
        }
        this.put(artifact.getName(), artifact);
        return true;
    }
}
