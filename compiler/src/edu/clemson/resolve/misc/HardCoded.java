package edu.clemson.resolve.misc;

import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.typereasoning.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.MTFunction;
import org.rsrg.semantics.MTFunction.MTFunctionBuilder;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.ScopeBuilder;

public class HardCoded {

    public static void addBuiltInSymbols(TypeGraph g, RESOLVECompiler rc,
            ScopeBuilder b) {
        try {
            b.addBinding("Cls", null, g.MTYPE, g.MTYPE);
            b.addBinding("SSet", null, g.MTYPE, g.SSET);
            b.addBinding("Entity", null, g.MTYPE, g.ENTITY);
            b.addBinding("B", null, g.SSET, g.BOOLEAN);

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
            b.addBinding("implies", null, new MTFunctionBuilder(g, g.BOOLEAN)
                    .paramTypes(g.BOOLEAN, g.BOOLEAN).build());

            /* b.addBinding("Is_Initial", null,
                     new MTFunctionBuilder(g, g.BOOLEAN).paramTypes(g.ENTITY)
                             .build());*/

            //S E T   R E L A T E D
            b.addBinding("Powerset", null, g.POWERSET);
            b.addBinding("union", null, g.UNION);
            b.addBinding("intersect", null, g.INTERSECT);
            b.addBinding("Empty_Set", null, g.SSET, g.EMPTY_SET);
            b.addBinding("||...||", null, new MTFunctionBuilder(g, g.CARD)
                    .paramTypes(g.SSET).build());
        }
        catch (DuplicateSymbolException e) {
            rc.errMgr.semanticError(ErrorKind.DUP_SYMBOL, null,
                    e.getExistingSymbol());
        }
    }

    //Todo: Should the following two methods *really* be in here?
    public static String getMetaFieldName(ParserRuleContext t) {
        String result;

        if ( t instanceof Resolve.MathFunctionExpContext ) {
            result = ((Resolve.MathFunctionExpContext) t).name.getText();
        }
        else if ( t instanceof Resolve.MathVariableExpContext ) {
            result = ((Resolve.MathVariableExpContext) t).name.getText();
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
