package resolvelite.semantics.symbol;

import resolvelite.semantics.Scope;
import resolvelite.semantics.Type;

public class VariableSymbol extends BaseSymbol implements TypedSymbol {

    public VariableSymbol(String name, Scope enclosingScope) {
        super(enclosingScope, name);
    }

    @Override public String toString() {
        String s = "";
        s = scope.getScopeDescription() + ".";
        if ( type != null ) {
            return '<' + s + getName() + "." + type + '>';
        }
        return s + getName();
    }

    @Override public Type getType() {
        return super.getType();
    }

    @Override public void setType(Type t) {
        super.setType(t);
    }

}
