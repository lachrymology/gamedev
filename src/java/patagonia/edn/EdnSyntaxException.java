package patagonia.edn;

/**
 * EdnSyntaxException is thrown when a syntax error is discovered
 * during parsing.
 */
public class EdnSyntaxException extends EdnException {
    private static final long serialVersionUID = 1L;

    public EdnSyntaxException() {
        super();
    }

    public EdnSyntaxException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public EdnSyntaxException(String msg) {
        super(msg);
    }

    public EdnSyntaxException(Throwable cause) {
        super(cause);
    }
}
