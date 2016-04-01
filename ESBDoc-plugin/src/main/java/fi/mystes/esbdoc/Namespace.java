package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 20.5.2015.
 */
public class Namespace {
    public final String PREFIX;
    public final String URI;

    public Namespace(String prefix, String uri){
        this.PREFIX = prefix;
        this.URI = uri;
    }
}
