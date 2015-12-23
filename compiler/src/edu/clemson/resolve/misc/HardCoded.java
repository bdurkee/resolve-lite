package edu.clemson.resolve.misc;

import edu.clemson.resolve.parser.ResolveParser;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.MTFunction;
import org.rsrg.semantics.MTFunction.MTFunctionBuilder;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.ScopeBuilder;

public class HardCoded {

    public static void addBuiltInSymbols(@NotNull TypeGraph g,
                                         @NotNull ScopeBuilder b) {
        try {
            b.addBinding("El", null, g.CLS, g.ELEMENT);
            b.addBinding("Cls", null, g.CLS, g.CLS);
            b.addBinding("SSet", null, g.CLS, g.SSET);

            b.addBinding("Entity", null, g.CLS, g.ENTITY);
            b.addBinding("B", null, g.SSET, g.BOOLEAN);
            b.addBinding("conc", null, g.SSET, g.BOOLEAN);

            b.addBinding("true", null, g.BOOLEAN);
            b.addBinding("false", null, g.BOOLEAN);
            b.addBinding("->", null, g.FUNCTION);
            b.addBinding("*", null, g.CROSS);

            b.addBinding("or", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.BOOLEAN, g.BOOLEAN).build());
            b.addBinding("and", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.BOOLEAN, g.BOOLEAN).build());
            b.addBinding("not", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.BOOLEAN).build());
            b.addBinding("=", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.ENTITY, g.ENTITY).build());
            b.addBinding("/=", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.ENTITY, g.ENTITY).build());
            b.addBinding("implies", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.BOOLEAN, g.BOOLEAN).build());
            b.addBinding("iff", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.BOOLEAN, g.BOOLEAN).build());

            //S E T   R E L A T E D
            b.addBinding("Finite_Powerset", null, g.POWERSET);
            b.addBinding("Powerset", null, g.POWERSET);

            b.addBinding("union", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.SSET, g.SSET).build());
            b.addBinding("intersect", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.SSET, g.SSET).build());
            b.addBinding("Empty_Set", null, g.SSET, g.EMPTY_SET);
            b.addBinding("||...||", null, new MTFunctionBuilder(g, g.NAT)
                    .paramTypes(g.SSET).build());
            b.addBinding("is_in", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.ENTITY, g.SSET).build());
            b.addBinding("is_not_in", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.ENTITY, g.SSET).build());
            b.addBinding("~", null, new MTFunctionBuilder(g, g.SSET)
                    .paramTypes(g.SSET, g.SSET).build());
        }
        catch (DuplicateSymbolException e) {
            throw new IllegalStateException("hardcoded symbol with a " +
                    "duplicate name: " + e.getExistingSymbol().getName());
        }
    }

    @NotNull public static String getMetaFieldName(@NotNull ParserRuleContext t) {
        return t.getText();
    }

    public static MTType getMetaFieldType(TypeGraph g, String metaSegment) {
        MTType result = null;

        if ( metaSegment.equals("Is_Initial") ) {
            result =
                    new MTFunction.MTFunctionBuilder(g, g.BOOLEAN).paramTypes(
                            g.ENTITY).build();
        }
        else if ( metaSegment.equals("base_point") ) {
            result = g.ENTITY;
        }
        return result;
    }
}
