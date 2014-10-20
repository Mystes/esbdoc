package fi.mystes.esbdoc;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.saxon.s9api.SaxonApiException;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import fi.mystes.esbdoc.handlers.ClassHandler;
import fi.mystes.esbdoc.handlers.ElementHandler;
import fi.mystes.esbdoc.handlers.FilterElseHandler;
import fi.mystes.esbdoc.handlers.FilterHandler;
import fi.mystes.esbdoc.handlers.IterateHandler;
import fi.mystes.esbdoc.handlers.ProxyHandler;
import fi.mystes.esbdoc.handlers.SequenceHandler;
import fi.mystes.esbdoc.handlers.StoreHandler;
import fi.mystes.esbdoc.handlers.SwitchCaseHandler;
import fi.mystes.esbdoc.handlers.SwitchDefaultHandler;
import fi.mystes.esbdoc.handlers.SwitchHandler;

public class SequenceDiagramBuilder {
	
	private String filesHome;
	private Map<String,String> visited;
	private ElementHandler elementHandler;
	
	public SequenceDiagramBuilder(String wso2Home) {
	    
	    filesHome = wso2Home + "/repository/deployment/server/synapse-configs/default/";
	    visited = new HashMap<String, String>();
	}
	
	private SequenceDiagramBuilder build(String file) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
		SAXParserFactory parserFactor = SAXParserFactory.newInstance();
		SAXParser parser = parserFactor.newSAXParser();
		parser.parse(new FileInputStream(filesHome + file), getNewHandler());
		
		return this;
	}
	
	public String buildPipe(String file){
		elementHandler = null;
		visited = new HashMap<String, String>();
		try {
			build(file);
			if (elementHandler != null) {
				return elementHandler.toString();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public SAXHandler getNewHandler() {
		return new SAXHandler(this);
	}

	/**
     * Main set up environment and generates all graphs. It uses WSO2_HOME as a starting
     * point where to find proxies and sequences.  
     * @param args 
	 * @throws SaxonApiException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
     */
    public static void main(String[] args){
    	
	    String result = new SequenceDiagramBuilder("/home/kreshnikg/Applications/wso2esb-4.5.1").buildPipe(args[6]);
	    System.out.println(result);
    }
    
    class SAXHandler extends DefaultHandler {

    	private String root;
    	private SequenceDiagramBuilder diagramBuilder;
    	private String simpleIteratorSplitExpression;
    	private String simpleIteratorTarget;
    	private AttributesImpl simpleIteratorAttributes;
    	
    	
    	public SAXHandler(SequenceDiagramBuilder builder) {
    		this.diagramBuilder = builder;
    		
    	}

		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			qName = qName.toLowerCase();
			// handle proxy file
			if (qName.equals("proxy")) {
				String proxyName = attributes.getValue("name");
				root = proxyName;
				visited.put(proxyName, "visited");
				createAndPrepareElementHandler(ProxyHandler.class, new Object[]{proxyName});
			} 
			// handle sequence
			else if (qName.equals("sequence")) {
				String sequenceName = attributes.getValue("name");
				String callableSequence = attributes.getValue("key");
				//handle sequence file
				if (sequenceName != null) {
					root = sequenceName;
					visited.put(sequenceName, "visited");
					createAndPrepareElementHandler(SequenceHandler.class, new Object[]{sequenceName});
				} 
				// handle callable sequence
				else if (callableSequence != null) {
					callSequenceOnDemand(callableSequence);
				} 
				// anonymous/in-line sequence needs no handling
				else {
					//System.out.println("Unhandled sequence type, skipping...");
				}
			} 
			// handle callable proxy
			else if (!(!qName.equals("customcallout") && !qName.equals("callout"))) {
				String service = attributes.getValue("serviceURL");
				if (service.startsWith("http")) {
                    // This should be URL so  resolve  target
					service = getUrlTargetNode(service);
                }
				createAndPrepareElementHandler(ProxyHandler.class, new Object[]{service});
				elementHandler.setCallee(root).setCallable(service);
				try {
					//in case of recursive proxy call
					if (!visited.containsKey(service)){
						diagramBuilder.build("proxy-services/"+service+"-1.0.0.xml");
					}
					
				} catch (Exception e) {
					//e.printStackTrace();
				}
			}
			// handler proxy's target's inSequence/outSequence
			else if (qName.equals("target") && elementHandler.getClass().isAssignableFrom(ProxyHandler.class)) {
				// handle inSequennce/outSequence attributes
				if (attributes.getValue("inSequence") != null){
					callSequenceOnDemand(attributes.getValue("inSequence"));
				}
				
				if (attributes.getValue("outSequence") != null){
					callSequenceOnDemand(attributes.getValue("outSequence"));
				}
			}
			// handle filter
			else if(qName.equals("filter")) {
				createAndPrepareElementHandler(FilterHandler.class, new Object[]{attributes});
			}
			// handle fitler's else
			else if(qName.equals("else")) {
				createAndPrepareElementHandler(FilterElseHandler.class, null);
			} 
			// handle switch
			else if(qName.equals("switch")) {
				createAndPrepareElementHandler(SwitchHandler.class, new Object[]{attributes});
			} 
			// handle switch's case
			else if(qName.equals("case")) {
				createAndPrepareElementHandler(SwitchCaseHandler.class, new Object[]{attributes});
			} 
			// handle switch's default
			else if(qName.equals("default")) {
				createAndPrepareElementHandler(SwitchDefaultHandler.class, null);
			}
			// handle faultsequence as filter
			else if(qName.equals("faultsequence")) {
				AttributesImpl attrs = new AttributesImpl();
				attrs.addAttribute(null, "xpath", "xpath", null, "SOAP fault occurred");
				createAndPrepareElementHandler(FilterHandler.class, new Object[]{attrs});
			}
			// handle iterate
			else if(qName.equals("iterate")) {
				createAndPrepareElementHandler(IterateHandler.class, new Object[]{attributes});
			}
			// target of iterate
			else if (qName.equals("target") && elementHandler.getClass().isAssignableFrom(IterateHandler.class)) {
				// handle sequence attributes
				if (attributes.getValue("sequence") != null){
					callSequenceOnDemand(attributes.getValue("sequence"));
				}
				
			}
			// handle simple iterator (custom)
			else if (qName.equals("property") && attributes.getValue("name") != null) {
				if (attributes.getValue("name").toLowerCase().equals("simpleiterator.splitexpression")){
					simpleIteratorSplitExpression = attributes.getValue("value");
						
				} else if(attributes.getValue("name").toLowerCase().equals("simpleiterator.target")) {
					simpleIteratorTarget = attributes.getValue("value");
				}
			}
			else if (qName.equals("spring") && attributes.getValue("bean") != null && attributes.getValue("bean").toLowerCase().equals("simpleiterator")) {
				if (simpleIteratorSplitExpression != null && simpleIteratorTarget != null) {
					simpleIteratorAttributes = new AttributesImpl();
					simpleIteratorAttributes.addAttribute(null, "expression", "expression", null, simpleIteratorSplitExpression);
					
					createAndPrepareElementHandler(IterateHandler.class, new Object[]{simpleIteratorAttributes});
					
					callSequenceOnDemand(simpleIteratorTarget);
					
					simpleIteratorAttributes = null;
					simpleIteratorSplitExpression = null;
					simpleIteratorTarget = null;
				}
			}
			// handle callable store
			else if(qName.toLowerCase().contains("store")) {
				createAndPrepareElementHandler(StoreHandler.class, null);
				if (elementHandler != null) {
					elementHandler.setCallee(root).setCallable(attributes.getValue("messageStore"));
				}
			} 
			//handle callable class
			else if(qName.equals("class")) {
				createAndPrepareElementHandler(ClassHandler.class, null);
				if (elementHandler != null) {
					elementHandler.setCallee(root).setCallable(attributes.getValue("name"));
				}
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			qName = qName.toLowerCase();
			if (!(!qName.equals("switch") 
					&& !qName.equals("default") 
					&& !qName.equals("case")
					&& !qName.equals("sequence") 
					&& !qName.equals("iterate") 
					&& !qName.equals("class")
					&& !qName.equals("filter") 
					&& !qName.equals("else") 
					&& !qName.equals("class") 
					&& !(qName.equals("spring") && simpleIteratorAttributes == null)
					&& !qName.equals("proxy")
					&& !(qName.equals("target")) 
					&& !qName.equals("store"))) {
				if (elementHandler != null && elementHandler.getParent() != null) {
					elementHandler = elementHandler.getParent();
				}
			}
		}
		
		private String getUrlTargetNode(String targ) {
	        try {
	            String target = null;
	            // This should be URL so  resolve  target
	            URL url = new URL(targ);
	            target = url.getFile();
	            target = target.substring(target.lastIndexOf("/") + 1, target.length());
	            //If it is  still on  notation xxx.yyy, use last part
	            if (target.lastIndexOf(".") > 0) {
	                target = target.substring(target.lastIndexOf(".") + 1, target.length());
	            }
	            return target;
	        } catch (MalformedURLException ex) {
	            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
	            return null;
	        }
	    }
		
		private void createAndPrepareElementHandler(Class<?> classToCreate, Object[] parameters) throws SAXException {
			
			try {
				ElementHandler handler = elementHandler;
				elementHandler = (ElementHandler)Class.forName(classToCreate.getName()).getConstructors()[0].newInstance(parameters);
				if (handler != null) {
					handler.addSubHandler(elementHandler);
				}
			} catch (Exception e) {
				throw new SAXException(e);
			} 
		}
		
		private void callSequenceOnDemand(String name) throws SAXException{
			createAndPrepareElementHandler(SequenceHandler.class, new Object[]{name});
			elementHandler.setCallee(root).setCallable(name);
			try {
				//in case of recursive sequence call
				if (!visited.containsKey(name)){
					diagramBuilder.build("sequences/"+name+"-1.0.0.xml");
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
    }
}
