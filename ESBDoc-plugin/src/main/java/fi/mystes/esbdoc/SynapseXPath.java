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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.xpath.AXIOMXPath;
import org.jaxen.JaxenException;

import static fi.mystes.esbdoc.Constants.ARTIFACT_DESCRIPTION_XPATH_STRING;
import static fi.mystes.esbdoc.Constants.SYNAPSE_NAMESPACE;

/**
 * Created by mystes-am on 20.5.2015.
 */
public class SynapseXPath extends AXIOMXPath {

    private SynapseXPath() throws JaxenException {
        super(ARTIFACT_DESCRIPTION_XPATH_STRING);
        this.addNamespace(SYNAPSE_NAMESPACE.PREFIX, SYNAPSE_NAMESPACE.URI);
    }

    public static Object evaluateOmElement(OMElement omElement) throws JaxenException{
        return new SynapseXPath().evaluate(omElement);
    }
}
