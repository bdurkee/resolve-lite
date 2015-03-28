package resolvelite.semantics;

public class NoSuchSymbolException extends Exception {

    public NoSuchSymbolException() {
        super();
    }

    public NoSuchSymbolException(String msg) {
        super(msg);
    }

    public NoSuchSymbolException(Exception causedBy) {
        super(causedBy);
    }
}
