package fi.mystes.esbdoc;

import java.util.Collection;
import java.util.TreeMap;

/**
 * Created by mystes-am on 29.5.2015.
 */
public class ArtifactMap extends TreeMap<String, Artifact> {
    public Collection<Artifact> getArtifacts(){
        return this.values();
    }
}
