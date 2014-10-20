package fi.mystes.esbdoc.handlers;

public class ProxyHandler extends ElementHandler {

	private String name;
	
	public ProxyHandler(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		// is this root handler?
		if (callee != null) {
			result.append(callee).append(" ->+ ")
				.append(name).append(":\n");
		}
		result.append(super.toString());
		// is this root handler?
		if (callee != null) {
			result.append(name)
			.append(" ->- ")
			.append(callee)
			.append(":\n");
		}
		return result.toString();
	}
}
