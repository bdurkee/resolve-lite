package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ResolveToken;
import org.resolvelite.misc.HardCoded;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTRecord;
import org.resolvelite.semantics.programtype.PTRepresentation;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.*;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ComputeTypes extends SetScopes {

    private static final TypeComparison<PSymbol, MTFunction> EXACT_DOMAIN_MATCH =
            new ExactDomainMatch();
    private static final Comparator<MTType> EXACT_PARAMETER_MATCH =
            new ExactParameterMatch();

    private final TypeComparison<PSymbol, MTFunction> INEXACT_DOMAIN_MATCH =
            new InexactDomainMatch();
    private final TypeComparison<PExp, MTType> INEXACT_PARAMETER_MATCH =
            new InexactParameterMatch();

    private final AnnotatedTree tr;
    private final TypeGraph g;
    protected int typeValueDepth = 0;

    protected boolean walkingMathDot = false;
    protected MTType currentSeg = null;

    ComputeTypes(ResolveCompiler rc, SymbolTable symtab,
            AnnotatedTree annotations) {
        super(rc, symtab);
        this.tr = annotations;
        this.g = symtab.getTypeGraph();
    }

    public void setCurrentScope(Scope s) {
        this.currentScope = s;
    }

    //-----------------------------------------------------------
    // BEGIN PROG EXP TYPING
    //-----------------------------------------------------------

    @Override public void exitProgNestedExp(
            @NotNull ResolveParser.ProgNestedExpContext ctx) {
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progExp()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progExp()));
    }

    @Override public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progPrimary()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            @NotNull ResolveParser.ProgPrimaryContext ctx) {
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.getChild(0)));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.getChild(0)));
    }

    @Override public void exitProgMemberExp(
            @NotNull ResolveParser.ProgMemberExpContext ctx) {
        ParseTree firstRecordRef = ctx.getChild(0);
        PTType first = tr.progTypes.get(firstRecordRef);

        //start by checking the first to ensure we're dealing with a record
        if ( !first.isAggregateType() ) {
            compiler.errorManager.semanticError(
                    ErrorKind.ILLEGAL_MEMBER_ACCESS, ctx.getStart(),
                    ctx.getText(), ctx.getChild(0).getText());
            tr.progTypes.put(ctx, PTInvalid.getInstance(g));
            tr.mathTypes.put(ctx, g.INVALID);
            return;
        }
        PTRepresentation curAggregateType = (PTRepresentation) first;

        //note this will represent the rightmost field type when finished.
        PTType curFieldType = curAggregateType;

        //now we need to make sure our mem accesses aren't nonsense.
        for (TerminalNode term : ctx.Identifier()) {
            PTRecord recordType = (PTRecord) curAggregateType.getBaseType();
            curFieldType = recordType.getFieldType(term.getText());
            if ( curFieldType == null ) {
                compiler.errorManager.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                        term.getSymbol(), term.getText());
                curFieldType = PTInvalid.getInstance(g);
                tr.progTypes.put(term, curFieldType);
                tr.mathTypes.put(term, curFieldType.toMath());
                break;
            }
            tr.progTypes.put(term, curFieldType);
            tr.mathTypes.put(term, curFieldType.toMath());

            if ( curFieldType.isAggregateType() ) {
                curAggregateType = (PTRepresentation) curFieldType;
            }
        }
        tr.progTypes.put(ctx, curFieldType);
        tr.mathTypes.put(ctx, curFieldType.toMath());
    }

    @Override public void exitProgNamedExp(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {
        try {
            ProgVariableSymbol variable =
                    currentScope.queryForOne(new ProgVariableQuery(
                            ctx.qualifier, ctx.name, true));
            tr.progTypes.put(ctx, variable.getProgramType());
            exitMathSymbolExp(ctx, ctx.qualifier, ctx.name.getText());
            return;
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(), ctx.name,
                    ctx.name.getText());
        }
        catch (UnexpectedSymbolException use) {
            compiler.errorManager.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.name, "a variable reference", ctx.name.getText(),
                    use.getActualSymbolDescription());
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
    }

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        PTType progType =
                DefSymbolsAndScopes.getProgramType(currentScope, compiler, ctx,
                        "Std_Integer_Fac", "Integer");
        tr.progTypes.put(ctx, progType);
        tr.mathTypes.put(ctx, g.Z);
    }

    @Override public void exitProgParamExp(
            @NotNull ResolveParser.ProgParamExpContext ctx) {
        typeOperationSym(ctx, ctx.qualifier, ctx.name, ctx.progExp());
    }

    @Override public void exitProgApplicationExp(
            @NotNull ResolveParser.ProgApplicationExpContext ctx) {
        typeOperationSym(ctx, new ResolveToken("Std_Integer_Fac"), ctx.op,
                ctx.progExp());
    }

    protected void typeOperationSym(ParserRuleContext ctx,
                                    Token qualifier, Token name,
                                    List<ResolveParser.ProgExpContext> args) {

        List<PTType> argTypes = args.stream().map(tr.progTypes::get)
                .collect(Collectors.toList());
        Token opAsName = Utils.getNameFromProgramOp(name.getText());
        try {
            OperationSymbol opSym = currentScope.queryForOne(
                    new OperationQuery(qualifier, opAsName, argTypes,
                            SymbolTable.FacilityStrategy.FACILITY_INSTANTIATE,
                            SymbolTable.ImportStrategy.IMPORT_NAMED));
            tr.progTypes.put(ctx, opSym.getReturnType());
            tr.mathTypes.put(ctx, opSym.getReturnType().toMath());
            return;
        }
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            List<String> argStrList = args.stream()
                    .map(ResolveParser.ProgExpContext::getText)
                    .collect(Collectors.toList());
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_OPERATION,
                    ctx.getStart(), name, argStrList, argTypes);
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
    }

    //-----------------------------------------------------------
    // BEGIN MATH EXP TYPING
    //-----------------------------------------------------------

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

    @Override public void exitConstraintClause(
            @NotNull ResolveParser.ConstraintClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitConventionClause(
            @NotNull ResolveParser.ConventionClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitCorrespondenceClause(
            @NotNull ResolveParser.CorrespondenceClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitRequiresClause(
            @NotNull ResolveParser.RequiresClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitEnsuresClause(
            @NotNull ResolveParser.EnsuresClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitMathNestedExp(
            @NotNull ResolveParser.MathNestedExpContext ctx) {
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

    @Override public void exitMathIntegerExp(
            @NotNull ResolveParser.MathIntegerExpContext ctx) {
        tr.mathTypes.put(ctx, g.Z);
    }

    @Override public void exitMathInfixExp(
            @NotNull ResolveParser.MathInfixExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, ctx.op, ctx.mathExp());
    }

    @Override public void exitMathOutfixExp(
            @NotNull ResolveParser.MathOutfixExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, new ResolveToken(ctx.lop.getText()
                + "..." + ctx.rop.getText()), ctx.mathExp());
    }

    @Override public void exitMathFunctionExp(
            @NotNull ResolveParser.MathFunctionExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, ctx.name, ctx.mathExp());
    }

    @Override public void enterMathCrossTypeExp(
            @NotNull ResolveParser.MathCrossTypeExpContext ctx) {
        typeValueDepth++;
    }

    /**
     * Simply represents the value of a math type that looks like the
     * following: {@code CART_PROD X1 : T1; X2 : T2; ... Xn : Tn; END;}
     */
    @Override public void exitMathCrossTypeExp(
            @NotNull ResolveParser.MathCrossTypeExpContext ctx) {
        List<MTCartesian.Element> fieldTypes = new ArrayList<>();

        for (ResolveParser.MathVariableDeclGroupContext grp : ctx
                .mathVariableDeclGroup()) {
            MTType grpType = tr.mathTypeValues.get(grp.mathTypeExp());
            for (TerminalNode t : grp.Identifier()) {
                fieldTypes.add(new MTCartesian.Element(t.getText(), grpType));
            }
        }
        tr.mathTypes.put(ctx, g.MTYPE);
        tr.mathTypeValues.put(ctx, new MTCartesian(g, fieldTypes));
        typeValueDepth--;
    }

    @Override public void exitMathSetCollectionExp(
            @NotNull ResolveParser.MathSetCollectionExpContext ctx) {
        tr.mathTypes.put(ctx, g.SSET);
        if (ctx.mathExp().isEmpty()) {
            tr.mathTypeValues.put(ctx, g.EMPTY_SET);
        }
        else {
            List<MTType> powersets = ctx.mathExp().stream()
                    .map(e -> new MTPowersetApplication(g, tr.mathTypes.get(e)))
                    .collect(Collectors.toList());
            MTUnion u = new MTUnion(g, powersets);
            tr.mathTypeValues.put(ctx, u);
        }
        if (typeValueDepth > 0) {

            // construct a union chain and see if all the component types
            // are known to contain only sets.
            List<MTType> elementTypes = ctx.mathExp().stream()
                    .map(tr.mathTypes::get).collect(Collectors.toList());

            MTUnion chainedTypes = new MTUnion(g, elementTypes);

            if (!chainedTypes.isKnownToContainOnlyMTypes() ||
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

    @Override public void exitMathSetBuilderExp(
            @NotNull ResolveParser.MathSetBuilderExpContext ctx) {
        tr.mathTypes.put(ctx, g.SSET);
        MTType singleParamType =
                tr.mathTypes.get(ctx.mathVariableDecl().mathTypeExp());
        MTType typeValue =
                new MTFunction.MTFunctionBuilder(g, g.POWERSET).paramTypes(
                        singleParamType).build();
        tr.mathTypeValues.put(ctx, typeValue);
    }

    @Override public void enterMathDotExp(
            @NotNull ResolveParser.MathDotExpContext ctx) {
        walkingMathDot = true;
    }

    /**
     * We aren't really doing so much typing here per-se as we are checking to
     * ensure each segment is in fact a cartesian.
     */
    @Override public void exitMathDotExp(
            @NotNull ResolveParser.MathDotExpContext ctx) {
        walkingMathDot = false;
        Iterator<ResolveParser.MathFunctionApplicationExpContext> segsIter =
                ctx.mathFunctionApplicationExp().iterator();
        ParserRuleContext nextSeg, lastSeg = null;
        if ( ctx.getStart().getText().equals("conc") )
            nextSeg = segsIter.next();
        nextSeg = segsIter.next();
        MTType curType = tr.mathTypes.get(nextSeg);
        MTCartesian curTypeCartesian;

        while (segsIter.hasNext()) {
            lastSeg = nextSeg;
            nextSeg = segsIter.next();
            String segmentName = HardCoded.getMetaFieldName(nextSeg);
            try {
                curTypeCartesian = (MTCartesian) curType;
                curType = curTypeCartesian.getFactor(segmentName);
            }
            catch (ClassCastException cce) {
                curType = HardCoded.getMetaFieldType(g, segmentName);
                if ( curType == null ) {
                    compiler.errorManager.semanticError(
                            ErrorKind.VALUE_NOT_TUPLE, nextSeg.getStart(),
                            segmentName);
                    curType = g.INVALID;
                    break;
                }
            }
            catch (NoSuchElementException nsee) {
                curType = HardCoded.getMetaFieldType(g, segmentName);
                if ( curType == null ) {
                    compiler.errorManager.semanticError(
                            ErrorKind.NO_SUCH_FACTOR, nextSeg.getStart(),
                            segmentName);
                    curType = g.INVALID;
                    break;
                }
            }
        }
        tr.mathTypes.put(ctx, curType);
        //Todo
        //tr.mathTypeValues.put(ctx, curType);
    }

    //Todo: This could use a redo/further refinement at some point. Though for
    //an initial pass it'll do.
    @Override public void exitMathEntailsAddendum(
            @NotNull ResolveParser.MathEntailsAddendumContext ctx) {

        List<Symbol> symbolsInThisScope = currentScope.query(
                new SymbolTypeQuery<>(Symbol.class));
        List<MathSymbol> coerceableMathSyms = new ArrayList<>();
        for (Symbol s : symbolsInThisScope) {
            try {
                coerceableMathSyms.add(s.toMathSymbol());
            } catch (UnexpectedSymbolException use) {}
        }
        List<String> renames = ctx.Identifier().stream()
                .map(ParseTree::getText).collect(Collectors.toList());
        for (ResolveParser.MathDotExpContext e : ctx.mathDotExp()) {
            int last = e.mathFunctionApplicationExp().size() - 1;
            renames.add(e.mathFunctionApplicationExp().get(last).getText());
        }
        MTType newType = tr.mathTypeValues.get(ctx.mathTypeExp());
        for (MathSymbol retypeSym : coerceableMathSyms) {
            if (renames.contains(retypeSym.getName())
                    && retypeSym.getModuleID().equals(tr.getName())) {
                retypeSym.setMathType(newType);
            }
        }
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        exitMathSymbolExp(ctx, null, ctx.name.getText());
    }

    private MathSymbol exitMathSymbolExp(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull String symbolName) {
        MathSymbol intendedEntry = null;
        try {
            intendedEntry = getIntendedEntry(qualifier, symbolName, ctx);
        }
        catch (NoSuchSymbolException e) {
            if ( walkingMathDot && currentSeg != null
                    && currentSeg instanceof MTCartesian ) {
                tr.mathTypes.put(ctx, currentSeg);
                return null; //just cut out.
            }
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                    ctx.getStart(), symbolName);
        }
        if ( intendedEntry == null ) {
            tr.mathTypes.put(ctx, g.INVALID);
        }
        else {
            tr.mathTypes.put(ctx, intendedEntry.getType());
            setSymbolTypeValue(ctx, symbolName, intendedEntry);
        }
        if ( walkingMathDot ) currentSeg = tr.mathTypes.get(ctx);
        return intendedEntry;
    }

    private MathSymbol getIntendedEntry(Token qualifier, String symbolName,
            ParserRuleContext ctx) throws NoSuchSymbolException {
        try {
            return currentScope.queryForOne(
                    new MathSymbolQuery(qualifier, symbolName, ctx.getStart()))
                    .toMathSymbol();
        }
        catch (DuplicateSymbolException dse) {
            throw new RuntimeException();
        }
    }

    private void setSymbolTypeValue(ParserRuleContext ctx, String symbolName,
            @NotNull MathSymbol intendedEntry) {
        try {
            if ( intendedEntry.getQuantification() == Quantification.NONE ) {
                tr.mathTypeValues.put(ctx, intendedEntry.getTypeValue());
            }
            else {
                if ( intendedEntry.getType().isKnownToContainOnlyMTypes() ) {
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
                currentScope //
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
            ResolveParser.MathExpContext... args) {
        typeMathFunctionLikeThing(ctx, qualifier, name, Arrays.asList(args));
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
        if ( typeValueDepth > 0 || name.getText().equals("Powerset")
                || name.getText().equals("union") ) {
            //  if ( typeValueDepth > 0 ) {

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
            MTType applicationType =
                    entryType.getApplicationType(intendedEntry.getName(),
                            arguments);
            tr.mathTypeValues.put(ctx, applicationType);
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
                        candidate.deschematize(e.getArguments(), currentScope);
                candidateType = (MTFunction) candidate.getType();
                compiler.info(candidate.getType() + " deschematizes to "
                        + candidateType);

                if ( comparison.compare(e, eType, candidateType) ) {
                    if ( match != null ) {
                        compiler.errorManager.semanticError(
                                ErrorKind.AMBIGIOUS_DOMAIN,
                                ((ParserRuleContext) candidate
                                        .getDefiningTree()).getStart(), match
                                        .getName(), match.getType(), candidate
                                        .getName(), candidate.getType());
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
                new PExpBuildingListener<T>(symtab.mathPExps,
                        symtab.quantifiedExps, tr);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
        return builder.getBuiltPExp(ctx);
    }
}
