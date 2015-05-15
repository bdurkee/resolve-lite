package org.resolvelite.semantics;

import java.util.List;
import java.util.Map;

public abstract class MTType {

    private final TypeGraph typeGraph;

    public MTType(TypeGraph typeGraph) {
        this.typeGraph = typeGraph;
    }

    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public abstract List<? extends MTType> getComponentTypes();

    public MTType getType() {
        //TODO : Each MTType should really contain it's declared type.  I.e.,
        //       if I say "Definition X : Set", I should store that X is
        //       of type Set someplace.  That's not currently available, so for
        //       the moment we say that all types are of type MType, the parent
        //       type of all types.
        return typeGraph.CLS;
    }

    public abstract void accept(TypeVisitor v);

    public abstract void acceptOpen(TypeVisitor v);

    public abstract void acceptClose(TypeVisitor v);

    /**
     * Indicates that this type is known to contain only elements <em>that
     * are themselves</em> types. Practically, this answers the question, "can
     * an instance of this type itself be used as a type?"
     */
    public boolean isKnownToContainOnlyMathTypes() {
        return false;
    }

    /**
     * Indicates that every instance of this type is itself known to contain
     * only elements that are types. Practically, this answers the question,
     * "if a function returns an instance of this type, can that instance itself
     * be said to contain only types?"
     */
    public boolean membersKnownToContainOnlyMathTypes() {
        return false;
    }

    public final MTType getCopyWithVariablesSubstituted(
            Map<String, MTType> substitutions) {
        VariableReplacingVisitor renamer =
                new VariableReplacingVisitor(substitutions);
        accept(renamer);
        return renamer.getFinalExpression();
    }

    @Override public boolean equals(Object o) {
        // System.err.println("mttype: " + this.toString() + " alph equiv to "
        //         + o.toString() + "?");
        if ( this == o ) {
            return true;
        }
        else {
            return false;
        }
    }
}
