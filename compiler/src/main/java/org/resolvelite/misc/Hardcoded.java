package org.resolvelite.misc;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.*;
import org.resolvelite.semantics.MTFunction.MTFunctionBuilder;
import org.resolvelite.typereasoning.TypeGraph;

public class HardCoded {

    public static void addBuiltInSymbols(TypeGraph g, ResolveCompiler rc,
            ScopeBuilder b) {
        try {
            b.addBinding("Cls", null, g.MTYPE, g.MTYPE);
            b.addBinding("SSet", null, g.MTYPE, g.SSET);
            b.addBinding("Empty_Set", null, g.SSET, g.EMPTY_SET);
            b.addBinding("Card", null, g.MTYPE, g.CARD);

            b.addBinding("Entity", null, g.MTYPE, g.ENTITY);
            b.addBinding("Conc", null, g.BOOLEAN, null);
            b.addBinding("B", null, g.SSET, g.BOOLEAN);
            b.addBinding("Z", null, g.SSET, g.Z);
            b.addBinding("N", null, g.SSET, g.N);

            b.addBinding("true", null, g.BOOLEAN);
            b.addBinding("false", null, g.BOOLEAN);
            b.addBinding("Powerset", null, g.POWERSET);
            b.addBinding("union", null, g.UNION);
            b.addBinding("min_int", null, g.Z, null);
            b.addBinding("max_int", null, g.Z, null);

            b.addBinding("+", null,
                    new MTFunctionBuilder(g, g.Z).paramTypes(g.Z, g.Z).build());
            b.addBinding("-", null,
                    new MTFunctionBuilder(g, g.Z).paramTypes(g.Z, g.Z).build());
            b.addBinding("<", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.CARD, g.CARD).build());
            b.addBinding("<=", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.CARD, g.CARD).build());
            b.addBinding(">", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.CARD, g.CARD).build());
            b.addBinding(">=", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.CARD, g.CARD).build());
            b.addBinding("=", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.ENTITY, g.ENTITY).build());
            b.addBinding("and", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.BOOLEAN, g.BOOLEAN).build());
            b.addBinding("||...||", null, new MTFunctionBuilder(g, g.CARD)
                    .paramTypes(g.SSET).build());
        }
        catch (DuplicateSymbolException e) {
            rc.errorManager.semanticError(ErrorKind.DUP_SYMBOL, null,
                    e.getExistingSymbol());
        }
    }

    //Todo: Should these *really* be in here?
    public static String getMetaFieldName(ParserRuleContext t) {
        String result;

        if ( t instanceof ResolveParser.MathFunctionExpContext ) {
            result = ((ResolveParser.MathFunctionExpContext) t).name.getText();
        }
        else if ( t instanceof ResolveParser.MathVariableExpContext ) {
            result = ((ResolveParser.MathVariableExpContext) t).name.getText();
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

        return result;
    }
}
