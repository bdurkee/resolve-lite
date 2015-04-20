package org.resolvelite.semantics.symbol;

import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.WeakHashMap;

public class MathInvalidSymbol extends MathSymbol {
    private static WeakHashMap<TypeGraph, MathInvalidSymbol> instances =
            new WeakHashMap<>();

    private MathInvalidSymbol(TypeGraph g, String name) {
        super(g, name, Quantification.NONE, null, g.MALFORMED, null, "");
    }

    public static MathInvalidSymbol getInstance(TypeGraph g, String name) {
        MathInvalidSymbol result = instances.get(g);
        if ( result == null ) {
            result = new MathInvalidSymbol(g, name);
            instances.put(g, result);
        }
        return result;
    }
}
