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

    public SynapseXPath() throws JaxenException {
        super(ARTIFACT_DESCRIPTION_XPATH_STRING);
        this.addNamespace(SYNAPSE_NAMESPACE.PREFIX, SYNAPSE_NAMESPACE.URI);
    }

    public static Object evaluateOmElement(OMElement omElement) throws JaxenException{
        return new SynapseXPath().evaluate(omElement);
    }
}
