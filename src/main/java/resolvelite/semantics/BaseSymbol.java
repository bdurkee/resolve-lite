package resolvelite.semantics;

public abstract class BaseSymbol implements Symbol {

    public enum Quantification {
        NONE {

            @Override
            public String toString() {
                return "None";
            }
        },
        UNIVERSAL {

            @Override
            public String toString() {
                return "Universal";
            }
        },
        EXISTENTIAL {

            @Override
            public String toString() {
                return "Existential";
            }
        }
    }

    protected final String name;
    protected Scope scope;
    protected int lexicalOrder;

    public BaseSymbol(String name) {
        this(null, name);
    }

    public BaseSymbol(Scope scope, String name) {
        this.scope = scope;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Scope getScope() {
        return scope;
    }

    @Override
    public void setScope(Scope scope) {
        this.scope = scope;
    }

    @Override
    public int getInsertionOrderNumber() {
        return lexicalOrder;
    }

    @Override
    public void setInsertionOrderNumber(int i) {
        this.lexicalOrder = i;
    }

    @Override
    public MathSymbol toMathSymbol() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName()
                + " cannot be coerced into a math symbol");
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof Symbol) ) {
            return false;
        }
        if ( obj == this ) {
            return true;
        }
        return name.equals(((Symbol) obj).getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        String s = "";
        if ( scope != null ) {
            s = scope.getScopeDescription() + ".";
        }
        return s + getName();
    }
}
