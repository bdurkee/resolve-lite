package org.resolvelite.semantics.symbol;

import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.Type;

import java.util.Map;

public abstract class BaseSymbol implements Symbol {

    public enum Quantification {
        NONE {

            @Override public String toString() {
                return "None";
            }
        },
        UNIVERSAL {

            @Override public String toString() {
                return "Universal";
            }
        },
        EXISTENTIAL {

            @Override public String toString() {
                return "Existential";
            }
        }
    }

    protected final String name, rootModuleID;
    protected Scope scope;
    protected int lexicalOrder;
    protected Type type;

    public BaseSymbol(String name, String rootModuleID) {
        this(null, name, rootModuleID);
    }

    public BaseSymbol(Scope scope, String name, String rootModuleID) {
        this.scope = scope;
        this.name = name;
        this.rootModuleID = rootModuleID;
    }

    /*  @Override public ProgTypeDefinitionSymbol toProgTypeDefSym()
              throws UnexpectedSymbolException {
          throw new UnexpectedSymbolException();
      }*/

    @Override public String getRootModuleID() {
        return rootModuleID;
    }

    @Override public String getName() {
        return name;
    }

    @Override public Scope getScope() {
        return scope;
    }

    @Override public void setScope(Scope scope) {
        this.scope = scope;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override public Symbol substituteGenerics(
            Map<GenericSymbol, Type> genericSubstitutions,
            Scope scopeWithSubstitutions) {
        return this;
    }

    @Override public boolean equals(Object obj) {
        if ( !(obj instanceof Symbol) ) {
            return false;
        }
        if ( obj == this ) {
            return true;
        }
        return name.equals(((Symbol) obj).getName());
    }

    @Override public int hashCode() {
        return name.hashCode();
    }

    @Override public String toString() {
        String s = "";
        if ( scope != null ) {
            s = scope.getScopeDescription() + ".";
        }
        return s + getName();
    }
}
