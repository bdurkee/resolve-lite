/*
 * [The "BSD license"]
 * Copyright (c) 2015 Clemson University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.ImportCollection;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.typereasoning.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.rsrg.semantics.*;
import org.rsrg.semantics.query.MathFunctionNamedQuery;
import org.rsrg.semantics.query.MathSymbolQuery;
import org.rsrg.semantics.symbol.MathSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.*;
import java.util.stream.Collectors;

public class PopulatingVisitor extends ResolveBaseVisitor<Void> {

    private static final TypeComparison<PSymbol, MTFunction> EXACT_DOMAIN_MATCH =
            new ExactDomainMatch();
    private static final Comparator<MTType> EXACT_PARAMETER_MATCH =
            new ExactParameterMatch();

    private final TypeComparison<PSymbol, MTFunction> INEXACT_DOMAIN_MATCH =
            new InexactDomainMatch();
    private final TypeComparison<PExp, MTType> INEXACT_PARAMETER_MATCH =
            new InexactParameterMatch();

    protected RESOLVECompiler compiler;
    protected SymbolTable symtab;
    protected AnnotatedTree tr;
    protected TypeGraph g;

    protected int typeValueDepth = 0;

    /**
     * Any quantification-introducing syntactic context (e.g., an
     * {@link edu.clemson.resolve.parser.Resolve.MathQuantifiedExpContext}),
     * introduces a level to this stack to reflect the quantification that
     * should be applied to named variables as they are encountered.
     * <p>
     * Note that this may change as the children of the node are processed;
     * for example, MathVariableDecls found in the declaration portion of a
     * quantified ctx should have quantification (universal or existential)
     * applied, while those found in the body of the quantified ctx QuantExp
     * no quantification (unless there is an embedded quantified ctx). In this
     * case, ctx should not remove its layer, but rather change it to
     * {@code Quantification.NONE}.</p>
     * <p>
     * This stack is never empty, but rather the bottom layer is always
     * {@code Quantification.NONE}.</p>
     */
    private Deque<Quantification> activeQuantifications = new LinkedList<>();

    public PopulatingVisitor(@NotNull RESOLVECompiler rc,
                   @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.activeQuantifications.push(Quantification.NONE);
        this.compiler = rc;
        this.symtab = symtab;
        this.tr = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    @Override public Void visitPrecisModule(
            @NotNull Resolve.PrecisModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tr.imports.getImportsOfType(ImportCollection.ImportType.NAMED));
        super.visitChildren(ctx);
        symtab.endScope();
        return null; //java requires a return, even if its 'Void'
    }

    @Override public Void visitMathCategoricalDefinitionDecl(
            @NotNull Resolve.MathCategoricalDefinitionDeclContext ctx) {
        for (Resolve.MathDefinitionSigContext sig : ctx.mathDefinitionSig()) {
            this.visit(sig);
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, sig.name.getText(),
                        tr.mathTypes.get(sig), null, ctx, getRootModuleID()));
            }
            catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        sig.name.getStart(), sig.name.getText());
            }
        }
        //visit the rhs of our categorical defn
        //this.visit(ctx.mathAssertionExp());   //Todo: Once we have func apps
        return null;
    }

    /**
     * Note: Shouldn't be calling this.visit(sigature.xxxx) anywhere at this
     * level. Those visits should all be taken care of in the visitor method
     * for the signature itself.
     */
    @Override public Void visitMathDefinitionDecl(
            @NotNull Resolve.MathDefinitionDeclContext ctx) {
        if (ctx.mathDefinitionSig().inductionVar != null) {
            System.err.println("illegal usage of induction variable in standard"
                    + " definition.");
        }
        Resolve.MathDefinitionSigContext sig = ctx.mathDefinitionSig();
        symtab.startScope(ctx);
        this.visit(sig);

        MTType defnType = tr.mathTypes.get(sig);
        MTType defnTypeValue = null;
        if (ctx.mathAssertionExp() != null) {
            //Note: We DO have to visit the rhs assertion explicitly here,
            //as it exists a level above the signature.
            this.visit(ctx.mathAssertionExp());
            defnTypeValue = tr.mathTypeValues.get(ctx.mathAssertionExp());
        }
        symtab.endScope();
        try {
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, sig.name.getText(),
                            defnType, defnTypeValue, ctx, getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    sig.name.getStart(), sig.name.getText());
        }
        return null;
    }

    /**
     * Since 'MathDefinitionSig' appears all over the place within our three
     * styles of definitions (categorical, standard, and inductive), we simply
     * use this signature visitor method to visit and type all relevant
     * children. Then, the top level definition nodes can simply grab the type
     * of the signature and build/populate the appropriate object. However,
     * know that in the defn top level nodes, we must remember to start scope,
     * visit the signature, and end scope. We don't do this in the signature
     * because certain information (rightfully) isn't present: defn body, etc.
     */
    @Override public Void visitMathDefinitionSig(
            @NotNull Resolve.MathDefinitionSigContext ctx) {
        //first visit the formal params
        activeQuantifications.push(Quantification.UNIVERSAL);
        ctx.mathVariableDeclGroup().forEach(this::visit);
        activeQuantifications.pop();

        //next, visit the definitions 'return type' to give it a type
        this.visit(ctx.mathTypeExp());

        //finally, build the full type of this definitions signature
        //If there are no params, then it is just the sym after the ':'
        MTType defnType = tr.mathTypeValues.get(ctx.mathTypeExp());
        MTFunction.MTFunctionBuilder builder =
                new MTFunction.MTFunctionBuilder(g, defnType);

        //if there are params, then our type needs to be an MTFunction.
        //this if check needs to be here or else, even if there were no params,
        //our type would end up MTFunction: Void -> T (which we don't want)
        if ( !ctx.mathVariableDeclGroup().isEmpty() ) {
            for (Resolve.MathVariableDeclGroupContext grp :
                    ctx.mathVariableDeclGroup()) {
                MTType grpType = tr.mathTypeValues.get(grp.mathTypeExp());
                for (TerminalNode t : grp.ID()) {
                    builder.paramTypes(grpType);
                    builder.paramNames(t.getText());
                }
            }
            //if the definition has parameters then it's type should be an
            //MTFunction (e.g. something like a * b ... -> ...)
            defnType = builder.build();
        }
        tr.mathTypes.put(ctx, defnType);
        return null;
    }

    @Override public Void visitMathVariableDeclGroup(
            @NotNull Resolve.MathVariableDeclGroupContext ctx) {
        for (TerminalNode t : ctx.ID()) {
            this.visit(ctx.mathTypeExp());
            MTType mathTypeValue = tr.mathTypeValues.get(ctx.mathTypeExp());
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, t.getText(), activeQuantifications
                                .peek(), mathTypeValue, null, ctx,
                                getRootModuleID()));
            }
            catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
        return null;
    }

    //---------------------------------------------------
    //  M A T H   E X P   V I S I T O R   M E T H O D S
    //---------------------------------------------------

    @Override public Void visitMathTypeExp(
            @NotNull Resolve.MathTypeExpContext ctx) {
        typeValueDepth++;
        this.visit(ctx.mathExp());
        typeValueDepth--;

        MTType type = tr.mathTypes.get(ctx.mathExp());
        MTType typeValue = tr.mathTypeValues.get(ctx.mathExp());
        if ( typeValue == null ) {
            compiler.errMgr.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
            typeValue = g.INVALID;
        }
        tr.mathTypes.put(ctx, type);
        tr.mathTypeValues.put(ctx, typeValue);
        return null;
    }

    @Override public Void visitMathNestedExp(
            @NotNull Resolve.MathNestedExpContext ctx) {
        this.visit(ctx.mathAssertionExp());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitMathInfixExp(
            @NotNull Resolve.MathInfixExpContext ctx) {
        ctx.mathExp().forEach(this::visit);
        typeMathFunctionLikeThing(ctx, null, ctx.op, ctx.mathExp());
        return null;
    }

    /*@Override public void exitMathOutfixExp(
            @NotNull ResolveParser.MathOutfixExpContext ctx) {
                    ctx.mathExp().forEach(this::visit);
        typeMathFunctionLikeThing(ctx, null, new ResolveToken(ctx.lop.getText()
                + "..." + ctx.rop.getText()), ctx.mathExp());
    }*/

    @Override public Void visitMathFunctionExp(
            @NotNull Resolve.MathFunctionExpContext ctx) {
        ctx.mathExp().forEach(this::visit);
        typeMathFunctionLikeThing(ctx, null, ctx.name, ctx.mathExp());
        return null;
    }

    @Override public Void visitMathTypeAssertionExp(
            @NotNull Resolve.MathTypeAssertionExpContext ctx) {
        if (typeValueDepth == 0) {
            this.visit(ctx.mathTypeExp());
        }
        this.visit(ctx.mathTypeExp());
        if ( typeValueDepth > 0 ) {
            try {
                //Todo: Check to ensure mathExp is in fact a variableExp
                MTType assertedType = tr.mathTypes.get(ctx.mathTypeExp());
                symtab.getInnermostActiveScope().addBinding(
                        ctx.mathExp().getText(), Quantification.UNIVERSAL,
                        ctx.mathExp(), tr.mathTypes.get(ctx.mathTypeExp()));

                tr.mathTypes.put(ctx, assertedType);
                tr.mathTypeValues.put(ctx,
                        new MTNamed(g, ctx.mathExp().getText()));

                //Note that a redudantly named type parameter would be
                //caught when we add a symbol to the symbol table, so no
                //need to check here
               // myDefinitionSchematicTypes.put(nodeExp.getName().getName(),
               //         node.getAssertedTy().getMathType());

                compiler.info("Added schematic variable: "
                        + ctx.mathExp().getText());
            }
            catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.mathExp().getStart(), ctx.mathExp().getText());
            }
        }
        return null;
    }

    @Override public Void visitMathAssertionExp(
            @NotNull Resolve.MathAssertionExpContext ctx) {
        this.visit(ctx.getChild(0));
        chainMathTypes(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathPrimeExp(
            @NotNull Resolve.MathPrimeExpContext ctx) {
        this.visit(ctx.mathPrimaryExp());
        chainMathTypes(ctx, ctx.mathPrimaryExp());
        return null;
    }

    @Override public Void visitMathPrimaryExp(
            @NotNull Resolve.MathPrimaryExpContext ctx) {
        this.visit(ctx.getChild(0));
        chainMathTypes(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathBooleanExp(
            @NotNull Resolve.MathBooleanExpContext ctx) {
        exitMathSymbolExp(ctx, null, ctx.getText());
        return null;
    }

    @Override public Void visitMathVariableExp(
            @NotNull Resolve.MathVariableExpContext ctx) {
        MathSymbol sym = exitMathSymbolExp(ctx, ctx.qualifier, ctx.getText());
        return null;
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
            return symtab.getInnermostActiveScope()
                    .queryForOne(new MathSymbolQuery(qualifier,
                            symbolName, ctx.getStart())).toMathSymbol();
        }
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(),
                    symbolName);
        }
        return null;
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
                compiler.errMgr
                        .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                ctx.getStart(), symbolName);
                tr.mathTypeValues.put(ctx, g.INVALID);
            }
        }
    }

    private void typeMathFunctionLikeThing(@NotNull ParserRuleContext ctx,
                               @Nullable Token qualifier, @NotNull Token name,
                               Resolve.MathExpContext... args) {
        typeMathFunctionLikeThing(ctx, qualifier, name, Arrays.asList(args));
    }

    private void typeMathFunctionLikeThing(@NotNull ParserRuleContext ctx,
                               @Nullable Token qualifier, @NotNull Token name,
                               List<Resolve.MathExpContext> args) {
        String foundExp = ctx.getText();
        MTFunction foundExpType;
        List<MTType> foundArgTypes =
                Utils.collect(MTType.class, args, tr.mathTypes);
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
            //  if ( typeValueDepth > 0 ) {

            //I had better identify a type
            MTFunction entryType = (MTFunction) intendedEntry.getType();

            List<MTType> arguments = new ArrayList<>();
            MTType argTypeValue;
            for (ParserRuleContext arg : args) {
                argTypeValue = tr.mathTypeValues.get(arg);
                if ( argTypeValue == null ) {
                    compiler.errMgr.semanticError(
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

    private MathSymbol getIntendedFunction(@NotNull ParserRuleContext ctx,
                               @Nullable Token qualifier, @NotNull Token name,
                               @NotNull List<Resolve.MathExpContext> args) {
        tr.mathTypes.put(ctx, PSymbol.getConservativePreApplicationType(g,
                args, tr.mathTypes));
        PSymbol e = (PSymbol)getPExpFor(ctx);
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
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MATH_FUNCTION,
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
                compiler.errMgr.semanticError(ErrorKind.NO_MATH_FUNC_FOR_DOMAIN,
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
                        compiler.errMgr.semanticError(
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
            //boolean result = g.isKnownToBeIn(foundValue, expectedType);
            return true;
         /*   if ( !result && foundValue instanceof PLambda
                    && expectedType instanceof MTFunction ) {
                PLambda foundValueAsLambda = (PLambda) foundValue;
                MTFunction expectedTypeAsFunction = (MTFunction) expectedType;
                MTFunction foundTypeAsFunction =
                        (MTFunction) foundValue.getMathType();
                result =
                        g.isSubtype(foundTypeAsFunction.getDomain(),
                                expectedTypeAsFunction.getDomain())
                                && g.isKnownToBeIn(
                                foundValueAsLambda.getBody(),
                                expectedTypeAsFunction.getRange());
            }*/
            // return result;
        }

        @Override public String description() {
            return "inexact";
        }
    }

    protected final PExp getPExpFor(@Nullable ParseTree ctx) {
        if ( ctx == null ) {
            return g.getTrueExp();
        }
        PExpBuildingListener<PExp> builder =
                new PExpBuildingListener<>(symtab.mathPExps, tr);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
        return builder.getBuiltPExp(ctx);
    }


    private void chainMathTypes(ParseTree current, ParseTree child) {
        tr.mathTypes.put(current, tr.mathTypes.get(child));
        tr.mathTypeValues.put(current, tr.mathTypeValues.get(child));
    }

    private String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }
}
