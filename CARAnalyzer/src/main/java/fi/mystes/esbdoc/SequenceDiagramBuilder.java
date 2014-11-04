package fi.mystes.esbdoc;

import java.io.File;
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
import org.xml.sax.helpers.DefaultHandler;

public class SequenceDiagramBuilder {
	
	private String filesHome;
	private Map<String,String> visited;
	private StringBuilder output;
	
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
		visited = new HashMap<String, String>();
		output = new StringBuilder();
		try {
			build(file);
			return output.toString();
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
    	
	    String result = new SequenceDiagramBuilder("/home/kreshnikg/Applications/wso2esb-4.5.1").buildPipe(args[7]);
	    System.out.println(result);
    }
    
    class SAXHandler extends DefaultHandler {

    	private String root;
    	private SequenceDiagramBuilder diagramBuilder;
    	private String simpleIteratorSplitExpression;
    	private String simpleIteratorTarget;
    	private boolean firstCase = true;
    	
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
				//createAndPrepareElementHandler(ProxyHandler.class, new Object[]{proxyName});
			}
			// handle endpoint file
			else if (qName.equals("endpoint")) {
				String name = attributes.getValue("name");
				String callable = attributes.getValue("key");
				//handle sequence file
				if (name != null) {
					root = name;
					visited.put(name, "visited");
					//createAndPrepareElementHandler(EndPointHandler.class, new Object[]{name});
				} 
				// handle callable endpoint
				else if (callable != null) {
					callEndPointOnDemand(callable);
				} 
				else {
					//System.out.println("Unhandled endpoint type, skipping...");
				}
			}
			else if (qName.equals("address")) {
				String uriTarget = attributes.getValue("uri");
				if (uriTarget.toLowerCase().contains("proxy")) {
					String proxy = getTargetFromEndPointURI(attributes.getValue("uri"));
					if (proxy != null) {
						callProxyOnDemand(proxy);
					} else {
						//System.out.println("Unhandled uri: " + attributes.getValue("uri"));
					}
				} else {
					callEndPointOnDemand("\""+uriTarget+"\"");
				}
			}
			// handle sequence
			else if (qName.equals("sequence")) {
				String sequenceName = attributes.getValue("name");
				String callableSequence = attributes.getValue("key");
				//handle sequence file
				if (sequenceName != null) {
					root = sequenceName;
					visited.put(sequenceName, "visited");
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
				callProxyOnDemand(service);
			}
			// handler proxy's target's inSequence/outSequence
			//else if (qName.equals("target") && elementHandler.getClass().isAssignableFrom(ProxyHandler.class)) {
			else if (qName.equals("target")) {
				// handle inSequennce/outSequence attributes
				// target of proxy
				if (attributes.getValue("inSequence") != null){
					callSequenceOnDemand(attributes.getValue("inSequence"));
				}
				//target of proxy
				if (attributes.getValue("outSequence") != null){
					callSequenceOnDemand(attributes.getValue("outSequence"));
				}
				// target of iterate mediator
				if (attributes.getValue("sequence") != null){
					callSequenceOnDemand(attributes.getValue("sequence"));
				}
			}
			// handle filter
			else if(qName.equals("filter")) {
				String condition = attributes.getValue("xpath");
				if (condition == null) {
					condition = attributes.getValue("source") + " == " + attributes.getValue("regex");
				}
				output.append("alt ").append(condition).append("\n");
			}
			// handle fitler's else
			else if(qName.equals("else")) {
				output.append("else\n");
			} 
			// handle switch
			else if(qName.equals("switch")) {
				output.append("alt ").append(attributes.getValue("source")).append("");
				firstCase = true;
			} 
			// handle switch's case
			else if(qName.equals("case")) {
				if (!firstCase) {
					output.append("else ");
				} else {
					output.append(" == ");
					firstCase = false;
				}
				output.append("\"").append(attributes.getValue("regex")).append("\"\n");
			} 
			// handle switch's default
			else if(qName.equals("default")) {
				output.append("else\n");
			}
			// handle faultsequence as filter
			else if(qName.equals("faultsequence")) {
				output.append("alt SOAP fault occurred\n");
			}
			// handle iterate
			else if(qName.equals("iterate")) {
				output.append("loop ").append(attributes.getValue("expression")).append("\n");
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
					callOnDemand(simpleIteratorTarget, null);
					simpleIteratorSplitExpression = null;
					simpleIteratorTarget = null;
				}
			}
			// handle callable store
			else if(qName.toLowerCase().contains("store")) {
				callOnDemand(attributes.getValue("messageStore"), null);
			} 
			//handle callable class
			else if(qName.equals("class")) {
				callOnDemand(attributes.getValue("name"), null);
			}
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);
			qName = qName.toLowerCase();
			
			if(!(!qName.equals("iterate") && !qName.equals("switch") && !qName.equals("filter") && !qName.equals("faultsequence"))) {
				output.append("end\n");
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
	            	int start = target.lastIndexOf(".") + 1;
	            	int end = target.length();
	            	if (target.toLowerCase().contains("endpoint")) {
	            		end = start -1;
	            		start = 0;
	            	}
	            	
	                target = target.substring(start, end);
	            }
	            return target;
	        } catch (MalformedURLException ex) {
	            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
	            return null;
	        }
	    }
		
		
		private void callSequenceOnDemand(String name) throws SAXException{
			callOnDemand(name, "sequences");
		}
		
		private void callProxyOnDemand(String name) throws SAXException{
			callOnDemand(name, "proxy-services");
		}
		
		private void callEndPointOnDemand(String name) throws SAXException{
			callOnDemand(name, "endpoints");
		}
		
		private void callOnDemand(String name, String dir) throws SAXException{
			output.append(root).append(" ->+ ").append(name).append(":\n");
			try {
				//in case of recursive sequence call
				if (!visited.containsKey(name)){
					String tempName = name + "-1.0.0.xml";
					if (fileExists(tempName, dir)) {
						diagramBuilder.build(dir + "/" +tempName);
					}
				}
				
			} catch (Exception e) {
				output.append(name).append(" ->- ").append(root).append(":\n");
				throw new SAXException(e);
			}
			output.append(name).append(" ->- ").append(root).append(":\n");
		}
		
		private boolean fileExists(String name, String dir){
			File file = new File(filesHome + "/" + dir + "/" + name);
			return file.exists() && file.isFile();
		}
		
		private String getTargetFromEndPointURI(String uri){
			//uri="http://localhost:8280/services/DeleteMacoContentsFromSSCOByQueueProxy"
			//uri="jms:/GetMacoContentProductKeyQueueProxy?transport.jms.ConnectionFactoryJ...
			if (uri.startsWith("http")) {
				return getUrlTargetNode(uri);
			} else if (uri.startsWith("jms")) {
				return uri.split("\\?")[0].split("/")[1];
			}
			return null;
		}
    }
}
