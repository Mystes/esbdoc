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

import org.apache.commons.lang3.ArrayUtils;
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
        HTTP, HTTPS, MAILTO, VFS, JMS, GOV, CONF, NULL
    }

    public static Scheme scheme(URI uri) throws EsbDocException {
        if(null == uri){
            log.warn("Null URI submitted. Cannot extract URI Scheme.");
            return Scheme.NULL;
        }

        String schemeString = uri.getScheme();
        if(StringUtils.isBlank(schemeString)){
            log.warn("Blank URI scheme for URI: " + uri.toString());
            return Scheme.NULL;
        }

        schemeString = schemeString.toUpperCase();

        if(!EnumUtils.isValidEnum(Scheme.class, schemeString)){
            StringBuilder sb = new StringBuilder();
            sb.append("Unrecognized URI scheme for URI: " + uri.toString());
            sb.append(". Unrecognized URI scheme was: " + schemeString);
            sb.append(". Supported schemes are: " + ArrayUtils.toString(Scheme.values()));
            throw new EsbDocException(sb.toString());
        }

        return Scheme.valueOf(schemeString);
    }
}
