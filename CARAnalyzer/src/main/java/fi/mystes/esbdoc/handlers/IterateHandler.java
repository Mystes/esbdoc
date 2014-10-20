package fi.mystes.esbdoc.handlers;

import org.xml.sax.Attributes;

public class IterateHandler extends ElementHandler{
	
	private String condition;
	
	public IterateHandler(Attributes attributes) {
		condition = attributes.getValue("expression");
	}

	@Override
	public String toString(){
		return new StringBuilder("loop ")
			   .append(condition)
			   .append("\n")
			   .append(super.toString())
			   .append("end\n")
			   .toString();
	}

}
