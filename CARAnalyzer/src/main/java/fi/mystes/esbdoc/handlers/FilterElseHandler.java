package fi.mystes.esbdoc.handlers;


public class FilterElseHandler extends ElementHandler {
	
	@Override
	public String toString() {
		return new StringBuilder("else\n")
				.append(super.toString()).toString();
	}
}
