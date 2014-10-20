package fi.mystes.esbdoc.handlers;

import org.xml.sax.Attributes;


public class FilterHandler extends ElementHandler {

	private String condition;
	
	public FilterHandler(Attributes attributes) {
		condition = attributes.getValue("xpath");
		if (condition == null) {
			condition = attributes.getValue("source") + " == " + attributes.getValue("regex");
		}
	}
	
	@Override
	public String toString() {
		return new StringBuilder("alt ").append(condition).append("\n").append(super.toString()).append("end\n").toString();
	}
}
