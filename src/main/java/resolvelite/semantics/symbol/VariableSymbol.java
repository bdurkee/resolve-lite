package resolvelite.semantics.symbol;

import resolvelite.semantics.Scope;
import resolvelite.semantics.Type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
}
