package org.resolvelite.semantics;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.typereasoning.TypeGraph;

public class ComputeMathTypes extends SetScopes {

    public ParseTreeProperty<MTType> mathTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<MTType> mathTypeValues = new ParseTreeProperty<>();
    protected TypeGraph g;

    ComputeMathTypes(@NotNull ResolveCompiler rc,
                     @NotNull SymbolTable symtab) {
        super(rc, symtab);
        this.g = symtab.getTypeGraph();
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        exitMathSymbolExp(null, ctx.getText(), ctx);
    }

    private MathSymbol exitMathSymbolExp(
            Token qualifier, String symbolName, ParseTree ctx) {
        MathSymbol intendedEntry = getIntendedEntry(qualifier, symbolName, ctx);
        mathTypes.put(ctx, intendedEntry.getType());
        setSymbolTypeValue(node, symbolName, intendedEntry);

        String typeValueDesc = "";
        if (mathTypeValues.get(ctx) != null) {
            typeValueDesc =
                    ", referencing math type " + mathTypeValues.get(ctx) + " ("
                            + mathTypeValues.get(ctx).getClass() + ")";
        }
        System.out.println("Processed symbol " + symbolName + " with type "
                + mathTypes.get(ctx) + typeValueDesc);
        return intendedEntry;
    }

    private MathSymbol getIntendedEntry(Token qualifier,
                                             String symbolName, ParseTree ctx) {
        try {
            return currentScope.queryForOne(
                            new MathSymbolQuery(qualifier, symbolName, node
                                    .getLocation()));
        }
        catch (DuplicateSymbolException dse) {
            throw new RuntimeException(); //This will never fire
        }
        catch (NoSuchSymbolException nsse) {
            noSuchSymbol(qualifier, symbolName, node.getLocation());
            throw new RuntimeException(); //This will never fire
        }
        return result;
    }
}
