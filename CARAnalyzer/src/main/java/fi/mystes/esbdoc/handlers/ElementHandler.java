package fi.mystes.esbdoc.handlers;

import java.util.LinkedList;
import java.util.List;

public abstract class ElementHandler {
	protected ElementHandler parent;
	protected List<ElementHandler> children = new LinkedList<ElementHandler>();
	protected String callable;
	protected String callee;

	/**
	 * 
	 * @param handler
	 * @return this for chaining 
	 */
	public ElementHandler addSubHandler(ElementHandler handler) {
		handler.setParent(this);
		children.add(handler);
		return this;
	}
	/**
	 * 
	 * @return parent
	 */
	public ElementHandler getParent() {
		return parent;
	}
	/**
	 * 
	 * @param parent
	 * @return this for chaining
	 */
	public ElementHandler setParent(ElementHandler parent) {
		this.parent = parent;
		return this;
	}
	
	public ElementHandler setCallable(String callable) {
		this.callable = callable;
		return this;
	}
	
	public ElementHandler setCallee(String callee) {
		this.callee = callee;
		return this;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(ElementHandler handler : children) {
			builder.append(handler.toString());
		}
		return builder.toString();
	}
}
