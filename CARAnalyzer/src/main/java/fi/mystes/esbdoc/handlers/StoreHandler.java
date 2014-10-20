package fi.mystes.esbdoc.handlers;

public class StoreHandler extends ElementHandler {
	
	@Override
	public String toString() {
		return new StringBuilder(callee).append(" ->+ ")
				.append(callable).append(":\n")
				.append(super.toString())
				.append(callable).append(" ->- ")
				.append(callee).append(":\n")
				.toString();
	}
}
