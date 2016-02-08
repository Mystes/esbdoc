package fi.mystes.esbdoc;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;

/**
 * Created by mystes-am on 8.2.2016.
 */
public class Protocol {
    private static Log log = LogFactory.getLog(Protocol.class);

    public enum Scheme {
        HTTP, HTTPS, MAILTO, VFS, JMS, GOV, CONF
    }

    public static Scheme scheme(URI uri){
        if(null == uri){
            log.warn("Null URI submitted. Cannot extract URI Scheme.");
            return null;
        }

        String schemeString = uri.getScheme();
        if(StringUtils.isBlank(schemeString)){
            log.warn("Blank URI scheme for URI: " + uri.toString());
            return null;
        }

        if(!EnumUtils.isValidEnum(Scheme.class, schemeString)){
            log.warn("Unrecognized URI scheme for URI: " + uri.toString());
            return null;
        }

        return Scheme.valueOf(schemeString.toUpperCase());
    }
}
