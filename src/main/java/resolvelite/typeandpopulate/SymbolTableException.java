package resolvelite.typeandpopulate;

@SuppressWarnings("serial")
public class SymbolTableException extends Exception {

    public SymbolTableException() {
        super();
    }

    public SymbolTableException(String msg) {
        super(msg);
    }

    public SymbolTableException(Exception e) {
        super(e);
    }
}