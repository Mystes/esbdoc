package fi.mystes.esbdoc.handlers;

import org.xml.sax.Attributes;


public class SwitchHandler extends ElementHandler{
	
	private String source;
	
	
	public SwitchHandler(Attributes attributes) {
		this.source = attributes.getValue("source");
	}

	@Override
	public ElementHandler addSubHandler(ElementHandler handler) {
		if(handler.getClass().isAssignableFrom(SwitchCaseHandler.class)) {
			((SwitchCaseHandler)handler).setSwitchCondition(source);
			if (children.isEmpty()) {
				((SwitchCaseHandler)handler).setPreCondition("alt");
			}
		} 
		return super.addSubHandler(handler);
	}

	@Override
	public String toString() {
		return new StringBuilder(super.toString()).append("end\n").toString();
	}

}
