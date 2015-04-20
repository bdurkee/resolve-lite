package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.query.MathSymbolQuery;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.semantics.symbol.Symbol.Quantification;
import org.resolvelite.typereasoning.TypeGraph;

public class ComputeMathTypes extends SetScopes {

    public ParseTreeProperty<MTType> mathTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<MTType> mathTypeValues = new ParseTreeProperty<>();
    protected TypeGraph g;
    protected int typeValueDepth = 0;

    ComputeMathTypes(@NotNull ResolveCompiler rc, @NotNull SymbolTable symtab) {
        super(rc, symtab);
        this.g = symtab.getTypeGraph();
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        exitMathSymbolExp(null, ctx.getText(), ctx);
    }

    @Override public void enterMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        typeValueDepth++;
    }

    @Override public void exitMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        typeValueDepth--;
    }

    private MathSymbol exitMathSymbolExp(Token qualifier, String symbolName,
            ParserRuleContext ctx) {
        MathSymbol intendedEntry = getIntendedEntry(qualifier, symbolName, ctx);

        mathTypes.put(ctx, intendedEntry.getType());
        setSymbolTypeValue(ctx, symbolName, intendedEntry);
        String typeValueDesc = "";

        if ( mathTypeValues.get(ctx) != null ) {
            typeValueDesc =
                    ", referencing math type " + mathTypeValues.get(ctx) + " ("
                            + mathTypeValues.get(ctx).getClass() + ")";
        }
        System.out.println("processed symbol " + symbolName + " with type "
                + mathTypes.get(ctx) + typeValueDesc);
        return intendedEntry;
    }

    private MathSymbol getIntendedEntry(Token qualifier, String symbolName,
            ParserRuleContext ctx) {
        try {
            return currentScope.queryForOne(new MathSymbolQuery(qualifier,
                    symbolName, ctx.getStart()));
        }
        catch (DuplicateSymbolException dse) {
            throw new RuntimeException(); //This will never fire
        }
        catch (NoSuchSymbolException nsse) {
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                    qualifier, symbolName);
            //return InvalidType.INSTANCE;
            return null;
        }
    }

    private void setSymbolTypeValue(ParserRuleContext ctx, String symbolName,
            @NotNull MathSymbol intendedEntry) {
        try {
            if ( intendedEntry.getQuantification() == Quantification.NONE ) {
                mathTypeValues.put(ctx, intendedEntry.getTypeValue());
            }
            else {
                if ( intendedEntry.getType().isKnownToContainOnlyMTypes() ) {
                    //mathTypeValues.put(ctx, new MTNamed(g, symbolName));
                }
            }
        }
        catch (SymbolNotOfKindTypeException snokte) {
            if ( typeValueDepth > 0 ) {
                //I had better identify a type
                compiler.errorManager
                        .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                ctx.getStart(), symbolName);
                // mathTypeValues.put(ctx, INVALID);
            }
        }
    }
}
