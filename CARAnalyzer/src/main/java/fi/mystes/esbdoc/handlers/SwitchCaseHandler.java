package fi.mystes.esbdoc.handlers;

import org.xml.sax.Attributes;

public class SwitchCaseHandler extends ElementHandler{

	private String switchCondition;
	private String checkCondition;
	private String preCondition = "else";
	
	public SwitchCaseHandler(Attributes attributes) {
		checkCondition = attributes.getValue("regex");
	}
	
	protected void setSwitchCondition(String switchCondition){
		this.switchCondition = switchCondition;
	}
	
	protected void setPreCondition(String preCondition) {
		this.preCondition = preCondition;
	}

	@Override
	public String toString() {
		return new StringBuilder(preCondition)
				.append(" ").append(switchCondition)
				.append(" == \"").append(checkCondition).append("\"\n")
				.append(super.toString()).toString();
	}

}
