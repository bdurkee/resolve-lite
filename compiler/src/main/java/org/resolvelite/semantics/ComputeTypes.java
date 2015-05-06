package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.MathFunctionNamedQuery;
import org.resolvelite.semantics.query.MathSymbolQuery;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.ProgTypeSymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// Todo: If we want to be more efficient about this we could use a visitor,
// which would allow us to be more specific as to which subexpressions we
// descend into (e.g.: those lacking types).
public class ComputeTypes extends ResolveBaseListener {

    private static final TypeComparison<PSymbol, MTFunction> EXACT_DOMAIN_MATCH =
            new ExactDomainMatch();
    private static final Comparator<MTType> EXACT_PARAMETER_MATCH =
            new ExactParameterMatch();

    private final TypeComparison<PSymbol, MTFunction> INEXACT_DOMAIN_MATCH =
            new InexactDomainMatch();
    private final TypeComparison<PExp, MTType> INEXACT_PARAMETER_MATCH =
            new InexactParameterMatch();

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

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        // tr.progTypes.put(ctx,
        //         getProgramType(ctx, "Std_Integer_Fac", "Integer"));
        tr.mathTypes.put(ctx, g.Z);
    }

    @Override public void exitMathInfixExp(
            @NotNull ResolveParser.MathInfixExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, ctx.op, ctx.mathExp());
    }

    @Override public void exitMathFunctionExp(
            @NotNull ResolveParser.MathFunctionExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, ctx.name, ctx.mathExp());
    }

    @Override public void exitMathSetCollectionExp(
            @NotNull ResolveParser.MathSetCollectionExpContext ctx) {
        tr.mathTypes.put(ctx, g.SSET);
        if (ctx.mathExp().isEmpty()) {
            tr.mathTypeValues.put(ctx, g.EMPTY_SET);
        }
        if (typeValueDepth > 0) {

            // construct a union chain and see if all the component types
            // are known to contain only sets.
            List<MTType> elementTypes = ctx.mathExp().stream()
                    .map(tr.mathTypes::get).collect(Collectors.toList());

            MTUnion chainedTypes = new MTUnion(g, elementTypes);

            if (!chainedTypes.isKnownToContainOnlyMathTypes() ||
                    ctx.mathExp().isEmpty()) {
                compiler.errorManager
                        .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                ctx.getStart(), ctx.getText());
                tr.mathTypeValues.put(ctx, g.INVALID);
                return;
            }
            tr.mathTypeValues.put(ctx, chainedTypes);
        }
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

    private MathSymbol getIntendedFunction(@NotNull ParserRuleContext ctx,
                                           @Nullable Token qualifier, @NotNull Token name,
                                           @NotNull List<ResolveParser.MathExpContext> args) {
        tr.mathTypes.put(ctx, PSymbol.getConservativePreApplicationType(g,
                args, tr.mathTypes));
        PSymbol e = buildPExp(ctx);
        MTFunction eType = (MTFunction)e.getMathType();
        String operatorStr = name.getText();

        List<MathSymbol> sameNameFunctions =
                symtab.getInnermostActiveScope() //
                        .query(new MathFunctionNamedQuery(qualifier, name))
                        .stream()
                        .filter(s -> s.getType() instanceof MTFunction)
                        .collect(Collectors.toList());

        List<MTType> sameNameFunctionTypes = sameNameFunctions.stream()
                .map(MathSymbol::getType).collect(Collectors.toList());

        if (sameNameFunctions.isEmpty()) {
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_MATH_FUNCTION,
                    ctx.getStart(), name.getText());
        }
        MathSymbol intendedFunction = null;
        try {
            intendedFunction = getExactDomainTypeMatch(e, sameNameFunctions);
        }
        catch (NoSolutionException nsee) {
            try {
                intendedFunction = getInexactDomainTypeMatch(e, sameNameFunctions);
            }
            catch (NoSolutionException nsee2) {
                compiler.errorManager.semanticError(ErrorKind.NO_MATH_FUNC_FOR_DOMAIN,
                        ctx.getStart(), eType.getDomain(), sameNameFunctions,
                        sameNameFunctionTypes);
            }
        }
        if (intendedFunction == null) return null;
        MTFunction intendedEntryType = (MTFunction) intendedFunction.getType();

        compiler.info("matching " + name.getText() + " : " + eType
                + " to " + intendedFunction.getName() + " : " + intendedEntryType);

        return intendedFunction;
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
        MathSymbol intendedEntry =
                getIntendedFunction(ctx, qualifier, name, args);

        if ( intendedEntry == null ) {
            tr.mathTypes.put(ctx, g.INVALID);
            return;
        }
        MTFunction expectedType = (MTFunction) intendedEntry.getType();

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
        }
    }

    protected final void chainMathTypes(ParseTree current, ParseTree child) {
        tr.mathTypes.put(current, tr.mathTypes.get(child));
        tr.mathTypeValues.put(current, tr.mathTypeValues.get(child));
    }

    private MathSymbol getExactDomainTypeMatch(PSymbol e,
            List<MathSymbol> candidates) throws NoSolutionException {
        return getDomainTypeMatch(e, candidates, EXACT_DOMAIN_MATCH);
    }

    private MathSymbol getInexactDomainTypeMatch(PSymbol e,
            List<MathSymbol> candidates) throws NoSolutionException {
        return getDomainTypeMatch(e, candidates, INEXACT_DOMAIN_MATCH);
    }

    private MathSymbol getDomainTypeMatch(PSymbol e,
            List<MathSymbol> candidates,
            TypeComparison<PSymbol, MTFunction> comparison)
            throws NoSolutionException {
        MTFunction eType = e.getConservativePreApplicationType(g);
        MathSymbol match = null;

        MTFunction candidateType;
        for (MathSymbol candidate : candidates) {
            try {
                candidate =
                        candidate.deschematize(e.getArguments(),
                                symtab.getInnermostActiveScope());
                candidateType = (MTFunction) candidate.getType();
                compiler.info(candidate.getType() + " deschematizes to "
                        + candidateType);

                if ( comparison.compare(e, eType, candidateType) ) {
                    if ( match != null ) {
                        compiler.errorManager.semanticError(
                                ErrorKind.AMBIGIOUS_DOMAIN, null,
                                match.getName(), match.getType(),
                                candidate.getName(), candidate.getType());
                    }
                    match = candidate;
                }
            }
            catch (NoSolutionException nse) {
                //couldn't deschematize--try the next one
                compiler.info(candidate.getType() + " doesn't deschematize "
                        + "against " + e.getArguments());
            }
        }
        if ( match == null ) {
            throw NoSolutionException.INSTANCE;
        }
        return match;
    }

    private static class ExactParameterMatch implements Comparator<MTType> {

        @Override public int compare(MTType o1, MTType o2) {
            int result;
            if ( o1.equals(o2) ) {
                result = 0;
            }
            else {
                result = 1;
            }
            return result;
        }
    }

    private static class ExactDomainMatch
            implements
                TypeComparison<PSymbol, MTFunction> {
        @Override public boolean compare(PSymbol foundValue,
                MTFunction foundType, MTFunction expectedType) {
            return foundType.parameterTypesMatch(expectedType,
                    EXACT_PARAMETER_MATCH);
        }

        @Override public String description() {
            return "exact";
        }
    }

    private class InexactDomainMatch
            implements
                TypeComparison<PSymbol, MTFunction> {

        @Override public boolean compare(PSymbol foundValue,
                MTFunction foundType, MTFunction expectedType) {
            return expectedType.parametersMatch(foundValue.getArguments(),
                    INEXACT_PARAMETER_MATCH);
        }

        @Override public String description() {
            return "inexact";
        }
    }

    private class InexactParameterMatch implements TypeComparison<PExp, MTType> {

        @Override public boolean compare(PExp foundValue, MTType foundType,
                MTType expectedType) {
            return g.isKnownToBeIn(foundValue, expectedType);
        }

        @Override public String description() {
            return "inexact";
        }
    }

    protected <T extends PExp> T buildPExp(ParserRuleContext ctx) {
        if ( ctx == null ) return null;
        PExpBuildingListener<T> builder =
                new PExpBuildingListener<T>(tr.mathTypes, tr.mathTypeValues);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
        return builder.getBuiltPExp(ctx);
    }
}
