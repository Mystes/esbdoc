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

        if (StringUtils.startsWith(this.uriString, "http")) {
            return parseHttpTarget(this.uriString);
        }

        if (StringUtils.startsWith(this.uriString, "jms")) {
            return parseJmsTarget(this.uriString);
        }

        return null;
    }

    //TODO This still needs refactoring
    private String parseHttpTarget(String parseThis) {
        try {
            String target = asFilename(parseThis);
            target = target.substring(target.lastIndexOf("/") + 1, target.length());
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

    private String asFilename(String parseThis) throws MalformedURLException {
        URL url = new URL(parseThis);
        return url.getFile();
    }

    //TODO This still needs refactoring
    private String parseJmsTarget(String parseThis){
        return parseThis.split("\\?")[0].split("/")[1];
    }
}
