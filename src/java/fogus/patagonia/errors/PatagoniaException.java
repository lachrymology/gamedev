package fogus.patagonia.errors;

public class PatagoniaException  extends Exception {
	private static final long serialVersionUID = -8547403154654596033L;

	public PatagoniaException() {
		super();
	}

	public PatagoniaException(String s) {
	    super(s);
	}
}
