package fi.mystes.esbdoc;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by mystes-am on 26.5.2015.
 */
public class EndpointURI {
    private static Log log = LogFactory.getLog(EndpointURI.class);

    private final String uriString;

    public EndpointURI(String uriString){
        this.uriString = uriString;
    }

    public String getTarget() {
        //uri="http://localhost:8280/services/DeleteMacoContentsFromSSCOByQueueProxy"
        //uri="jms:/GetMacoContentProductKeyQueueProxy?transport.jms.ConnectionFactoryJ...

        if (StringUtils.startsWith(uriString, "http")) {
            return parseHttpTarget(uriString);
        }

        if (StringUtils.startsWith(uriString, "jms")) {
            return parseJmsTarget(uriString);
        }

        return null;
    }

    private String parseHttpTarget(String targ) {
        try {
            String target = null;
            // This should be URL so  resolve  target
            URL url = new URL(targ);
            target = url.getFile();
            target = target.substring(target.lastIndexOf("/") + 1, target.length());
            //If it is  still on  notation xxx.yyy, use last part
            if (target.lastIndexOf(".") > 0) {
                int start = target.lastIndexOf(".") + 1;
                int end = target.length();
                if (target.toLowerCase().contains("endpoint")) {
                    end = start - 1;
                    start = 0;
                }
                target = target.substring(start, end);
            }
            return target;
        } catch (MalformedURLException ex) {
            log.error(ex);
            return null;
        }
    }

    private String parseJmsTarget(String target){
        return target.split("\\?")[0].split("/")[1];
    }
}
