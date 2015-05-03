package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.MathSymbolQuery;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.ProgTypeSymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.List;

public class ComputeTypes extends ResolveBaseListener {

    private final AnnotatedTree tr;
    private final ResolveCompiler compiler;
    private final TypeGraph g;
    private final SymbolTable symtab;
    protected int typeValueDepth = 0;

    ComputeTypes(ResolveCompiler rc, SymbolTable symtab,
            AnnotatedTree annotations) {
        this.tr = annotations;
        this.compiler = rc;
        this.symtab = symtab;
        this.g = symtab.getTypeGraph();
    }

    @Override public void enterMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        typeValueDepth++;
    }

    @Override public void exitMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        typeValueDepth--;

        MTType type = tr.mathTypes.get(ctx.mathExp());
        MTType typeValue = tr.mathTypeValues.get(ctx.mathExp());
        if ( typeValue == null ) {
            compiler.errorManager.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
            typeValue = g.INVALID; // not a type? let's give it an invalid value then
        }
        tr.mathTypes.put(ctx, type);
        tr.mathTypeValues.put(ctx, typeValue);
    }

    @Override public void exitRequiresClause(
            @NotNull ResolveParser.RequiresClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitEnsuresClause(
            @NotNull ResolveParser.EnsuresClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitMathAssertionExp(
            @NotNull ResolveParser.MathAssertionExpContext ctx) {
        chainMathTypes(ctx, ctx.getChild(0));
    }

    @Override public void exitMathPrimeExp(
            @NotNull ResolveParser.MathPrimeExpContext ctx) {
        chainMathTypes(ctx, ctx.mathPrimaryExp());
    }

    @Override public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        chainMathTypes(ctx, ctx.getChild(0));
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        exitMathSymbolExp(ctx, null, ctx.getText());
    }

    @Override public void exitMathInfixExp(
            @NotNull ResolveParser.MathInfixExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, ctx.op, ctx.mathExp());
    }

    @Override public void exitMathFunctionExp(
            @NotNull ResolveParser.MathFunctionExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, ctx.name, ctx.mathExp());
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        exitMathSymbolExp(ctx, null, ctx.name.getText());
    }

    private MathSymbol exitMathSymbolExp(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull String symbolName) {
        MathSymbol intendedEntry = getIntendedEntry(qualifier, symbolName, ctx);
        if ( intendedEntry == null ) {
            tr.mathTypes.put(ctx, g.INVALID);
        }
        else {
            tr.mathTypes.put(ctx, intendedEntry.getType());
            setSymbolTypeValue(ctx, symbolName, intendedEntry);

            MTType typeValue = tr.mathTypeValues.get(ctx);
            MTType type = tr.mathTypes.get(ctx);
            String typeValueDesc = "";

            if ( typeValue != null ) {
                typeValueDesc =
                        ", referencing math type " + typeValue + " ("
                                + typeValue.getClass() + ")";
            }
            compiler.info("processed symbol " + symbolName + " with type "
                    + type + typeValueDesc);
        }
        return intendedEntry;
    }

    private MathSymbol getIntendedEntry(Token qualifier, String symbolName,
            ParserRuleContext ctx) {
        try {
            return symtab
                    .getInnermostActiveScope()
                    .queryForOne(
                            new MathSymbolQuery(qualifier, symbolName, ctx
                                    .getStart())).toMathSymbol();
        }
        catch (DuplicateSymbolException dse) {
            throw new RuntimeException();
        }
        catch (NoSuchSymbolException nsse) {
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                    ctx.getStart(), symbolName);
            return null;
        }
    }

    private void setSymbolTypeValue(ParserRuleContext ctx, String symbolName,
            @NotNull MathSymbol intendedEntry) {
        try {
            if ( intendedEntry.getQuantification() == Symbol.Quantification.NONE ) {
                tr.mathTypeValues.put(ctx, intendedEntry.getTypeValue());
            }
            else {
                if ( intendedEntry.getType().isKnownToContainOnlyMathTypes() ) {
                    tr.mathTypeValues.put(ctx, new MTNamed(g, symbolName));
                }
            }
        }
        catch (SymbolNotOfKindTypeException snokte) {
            if ( typeValueDepth > 0 ) {
                //I had better identify a type
                compiler.errorManager
                        .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                ctx.getStart(), symbolName);
                tr.mathTypeValues.put(ctx, g.INVALID);
            }
        }
    }

    private void typeMathFunctionLikeThing(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull Token name,
            List<ResolveParser.MathExpContext> args) {
        MTFunction foundExpType;
        foundExpType =
                PSymbol.getConservativePreApplicationType(g, args, tr.mathTypes);

        compiler.info("expression: " + ctx.getText() + "("
                + ctx.getStart().getLine() + ","
                + ctx.getStop().getCharPositionInLine() + ") of type "
                + foundExpType.toString());
        MathSymbol intendedEntry = null;
        // getIntendedFunction(ctx, qualifier, name, args);

        // if ( intendedEntry == null ) {
        tr.mathTypes.put(ctx, g.INVALID);
        //return;
        // }
        /* MTFunction expectedType = (MTFunction) intendedEntry.getType();

         //We know we match expectedType--otherwise the above would have thrown
         //an exception.
         tr.mathTypes.put(ctx, expectedType.getRange());

         if ( typeValueDepth > 0 ) {
             //I had better identify a type
             MTFunction entryType = (MTFunction) intendedEntry.getType();

             List<MTType> arguments = new ArrayList<>();
             MTType argTypeValue;
             for (ParserRuleContext arg : args) {
                 argTypeValue = tr.mathTypeValues.get(arg);
                 if ( argTypeValue == null ) {
                     compiler.errorManager.semanticError(
                             ErrorKind.INVALID_MATH_TYPE, arg.getStart(),
                             arg.getText());
                 }
                 arguments.add(argTypeValue);
             }
             tr.mathTypeValues.put(ctx, entryType.getApplicationType(
                     intendedEntry.getName(), arguments));
         }*/
    }

    protected final void chainMathTypes(ParseTree current, ParseTree child) {
        tr.mathTypes.put(current, tr.mathTypes.get(child));
        tr.mathTypeValues.put(current, tr.mathTypeValues.get(child));
    }
}
