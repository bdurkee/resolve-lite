package edu.clemson.resolve.misc;

import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.Resolve;
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
            b.addBinding("El", null, g.MTYPE, g.ELEMENT);
            b.addBinding("Cls", null, g.MTYPE, g.MTYPE);
            b.addBinding("SSet", null, g.MTYPE, g.SSET);
            b.addBinding("SStr", null, g.SSET, g.SSTR);

            b.addBinding("Entity", null, g.MTYPE, g.ENTITY);
            b.addBinding("B", null, g.SSET, g.BOOLEAN);
            b.addBinding("N", null, g.SSET, g.NAT);
            b.addBinding("Z", null, g.SSET, g.Z);

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

            /* b.addBinding("Is_Initial", null,
                     new MTFunctionBuilder(g, g.BOOLEAN).paramTypes(g.ENTITY)
                             .build());*/

            //S T R I N G   R E L A T E D
            b.addBinding("Str", null, g.STR);
            b.addBinding("Empty_String", null, g.SSTR, g.EMPTY_STRING);
            b.addBinding("|...|", null, new MTFunctionBuilder(g, g.Z)
                    .paramTypes(g.SSET).build());
            b.addBinding("<...>", null, new MTFunctionBuilder(g, g.SSTR)
                    .paramTypes(g.SSTR).build());
            b.addBinding("o", null, g.STR_CAT);

            //S E T   R E L A T E D
            b.addBinding("Finite_Powerset", null, g.POWERSET);
            b.addBinding("Powerset", null, g.POWERSET);

            b.addBinding("union", null, g.UNION);
            b.addBinding("intersect", null, g.INTERSECT);
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
        String result = "";
        if ( t instanceof Resolve.MathSymbolExpContext ) {
            result = ((Resolve.MathSymbolExpContext) t).name.getText();
        }
        else {
            throw new RuntimeException("not a variable exp or function exp: "
                    + t.getText() + " (" + t.getClass() + ")");
        }
        return result;
    }

    public static MTType getMetaFieldType(TypeGraph g, String metaSegment) {
        MTType result = null;

        if ( metaSegment.equals("Is_Initial") ) {
            result =
                    new MTFunction.MTFunctionBuilder(g, g.BOOLEAN).paramTypes(
                            g.ENTITY).build();
        }
        else if ( metaSegment.equals("Base_Point") ) {
            result = new MTFunction.MTFunctionBuilder(g, g.ENTITY).build();
        }
        return result;
    }
}
