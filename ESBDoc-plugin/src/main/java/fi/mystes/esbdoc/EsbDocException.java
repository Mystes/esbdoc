package fi.mystes.esbdoc;

/**
 * Created by mystes-am on 17.2.2016.
 */
public class EsbDocException extends RuntimeException {
    //TODO perhaps later make this a checked exception?
    public EsbDocException(String message) {
        super(message);
    }
}