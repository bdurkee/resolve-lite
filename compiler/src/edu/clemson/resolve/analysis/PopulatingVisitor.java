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
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.HardCoded;
import edu.clemson.resolve.misc.HardCodedProgOps;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import edu.clemson.resolve.proving.absyn.PSymbol;
import org.rsrg.semantics.TypeGraph;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.*;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.*;
import org.rsrg.semantics.query.*;
import org.rsrg.semantics.symbol.*;

import java.util.*;
import java.util.stream.Collectors;

public class PopulatingVisitor extends ResolveBaseVisitor<Void> {

    private static final boolean EMIT_DEBUG = false;
    private static final TypeComparison<PSymbol, MTFunction> EXACT_DOMAIN_MATCH =
            new ExactDomainMatch();
    private static final Comparator<MTType> EXACT_PARAMETER_MATCH =
            new ExactParameterMatch();

    private final TypeComparison<PSymbol, MTFunction> INEXACT_DOMAIN_MATCH =
            new InexactDomainMatch();
    private final TypeComparison<PExp, MTType> INEXACT_PARAMETER_MATCH =
            new InexactParameterMatch();

    private boolean walkingDefParams = false;

    /**
     * Keeps track of the current operationProcedure we're visiting;
     * {@code null} otherwise. We use this to check whether a recursive call is
     * being made to an operation procedure decl that hasn't been marked
     * 'Recursive'.
     */
    private Resolve.OperationProcedureDeclContext currentOpProcedureDecl = null;

    /**
     * Set to {@code true} when we're walking the arguments to a module
     * (i.e. walking some set of args to a facility decl); or when we're walking
     * module formal parameters. Should be {@code false} otherwise;
     */
    private boolean walkingModuleArgOrParamList = false;

    /**
     * Keeps track of an inductive defn's (top level declared) induction
     * variable for access later in the (lower level) signature. This is
     * {@code null} if we're not visiting the children of an inductive defn.
     */
    private Resolve.MathVariableDeclContext currentInductionVar = null;

    private Map<String, MTType> definitionSchematicTypes = new HashMap<>();

    private ProgTypeModelSymbol currentTypeModelSym = null;

    private RESOLVECompiler compiler;
    private SymbolTable symtab;
    private AnnotatedTree tr;
    private TypeGraph g;

    private int typeValueDepth = 0;
    private int globalSpecCount = 0;

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

    private ModuleScopeBuilder moduleScope = null;

    public PopulatingVisitor(RESOLVECompiler rc,
                     SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.activeQuantifications.push(Quantification.NONE);
        this.compiler = rc;
        this.symtab = symtab;
        this.tr = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    @Override public Void visitModule(Resolve.ModuleContext ctx) {
        moduleScope = symtab.startModuleScope(ctx, Utils.getModuleName(ctx))
                .addImports(tr.semanticallyVisibleUses);
        super.visitChildren(ctx);
        symtab.endScope();
        return null; //java requires a return, even if its 'Void'
    }

    @Override public Void visitConceptImplModule(
            Resolve.ConceptImplModuleContext ctx) {
        moduleScope.addDependentTerms(symtab.moduleScopes.get(
                ctx.concept.getText()).getDependentTerms())
                .addParentSpecificationRelationship(ctx.concept.getText());
        super.visitChildren(ctx);
        return null;
    }

    @Override public Void visitEnhancementImplModule(
            Resolve.EnhancementImplModuleContext ctx) {
        moduleScope
                .addDependentTerms(symtab.moduleScopes.get(ctx.concept.getText()).getDependentTerms())
                .addDependentTerms(symtab.moduleScopes.get(ctx.enhancement.getText()).getDependentTerms())
                .addParentSpecificationRelationship(ctx.concept.getText());
        super.visitChildren(ctx);
        return null;
    }

    @Override public Void visitImplModuleParameterList(
            Resolve.ImplModuleParameterListContext ctx) {
        walkingModuleArgOrParamList = true;
        this.visitChildren(ctx);
        walkingModuleArgOrParamList = false;
        return null;
    }

    @Override public Void visitSpecModuleParameterList(
            Resolve.SpecModuleParameterListContext ctx) {
        walkingModuleArgOrParamList = true;
        this.visitChildren(ctx);
        walkingModuleArgOrParamList = false;
        return null;
    }


    @Override public Void visitDependentTermOptions(
            Resolve.DependentTermOptionsContext ctx) {
        moduleScope.addDependentTerms(ctx.ID().stream()
                .map(TerminalNode::getText).collect(Collectors.toList()));
        return null;
    }

    @Override public Void visitGenericType(Resolve.GenericTypeContext ctx) {
        try {
            symtab.getInnermostActiveScope().define(
                    new GenericSymbol(g, new PTElement(g), ctx.getText(),
                            ctx, getRootModuleID()));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.ID().getText());
        }
        return null;
    }

    @Override public Void visitTypeModelDecl(Resolve.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
        this.visit(ctx.mathTypeExp());
        MathSymbol exemplarSymbol = null;
        try {
            exemplarSymbol =
                    symtab.getInnermostActiveScope().addBinding(
                            ctx.exemplar.getText(), ctx,
                            tr.mathTypeValues.get(ctx.mathTypeExp()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.getText());
        }
        if (ctx.constraintClause() != null) this.visit(ctx.constraintClause());
        if (ctx.typeModelInit() != null) this.visit(ctx.typeModelInit());
        symtab.endScope();
        try {
            PExp constraint =
                    getPExpFor(ctx.constraintClause() != null ? ctx
                            .constraintClause() : null);
            PExp initEnsures =
                    getPExpFor(ctx.typeModelInit() != null ? ctx
                            .typeModelInit().ensuresClause() : null);
            MTType modelType = tr.mathTypeValues.get(ctx.mathTypeExp());

            ProgTypeSymbol progType =
                    new ProgTypeModelSymbol(symtab.getTypeGraph(),
                            ctx.name.getText(), modelType,
                                new PTFamily(modelType, ctx.name.getText(),
                                    ctx.exemplar.getText(), constraint,
                                    initEnsures, getRootModuleID()),
                            exemplarSymbol, ctx, getRootModuleID());
            symtab.getInnermostActiveScope().define(progType);
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
        return null;
    }

    @Override public Void visitProcedureDecl(Resolve.ProcedureDeclContext ctx) {
        OperationSymbol correspondingOp = null;
        try {
            correspondingOp =
                    symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(null, ctx.name, false))
                            .toOperationSymbol();
        } catch (NoSuchSymbolException nse) {
            compiler.errMgr.semanticError(ErrorKind.DANGLING_PROCEDURE,
                    ctx.getStart(), ctx.name.getText());
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.getText());
        }
        symtab.startScope(ctx);
        this.visit(ctx.operationParameterList());
        PTType returnType = null;
        if (ctx.type() != null) {
            this.visit(ctx.type());
            returnType = tr.progTypeValues.get(ctx.type());
            try {
                symtab.getInnermostActiveScope().define(
                        new ProgVariableSymbol(ctx.name.getText(), ctx,
                                returnType, getRootModuleID()));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                        ctx.name.getText());
            }
        }
        else {
            returnType = PTVoid.getInstance(g);
        }
        ctx.variableDeclGroup().forEach(this::visit);
        ctx.stmt().forEach(this::visit);
        symtab.endScope();
        try {
            symtab.getInnermostActiveScope().define(
                    new ProcedureSymbol(ctx.name.getText(), ctx,
                            getRootModuleID(), correspondingOp));
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.name.getText());
        }
        return null;
    }

    @Override public Void visitOperationDecl(Resolve.OperationDeclContext ctx) {
        symtab.startScope(ctx);
        ctx.operationParameterList().parameterDeclGroup().forEach(this::visit);
        if (ctx.type() != null) {
            this.visit(ctx.type());
            try {
                symtab.getInnermostActiveScope().addBinding(ctx.name.getText(),
                        ctx.getParent(), tr.mathTypeValues.get(ctx.type()));
            } catch (DuplicateSymbolException e) {
                //This shouldn't be possible--the operation declaration has a
                //scope all its own and we're the first ones to get to
                //introduce anything
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.getStart(), ctx.getText());
            }
        }
        if (ctx.requiresClause() != null) this.visit(ctx.requiresClause());
        if (ctx.ensuresClause() != null) this.visit(ctx.ensuresClause());
        symtab.endScope();
        insertFunction(ctx.name, ctx.type(),
                ctx.requiresClause(), ctx.ensuresClause(), ctx);
        return null;
    }

    @Override public Void visitOperationProcedureDecl(
            Resolve.OperationProcedureDeclContext ctx) {
        symtab.startScope(ctx);
        currentOpProcedureDecl = ctx;
        ctx.operationParameterList().parameterDeclGroup().forEach(this::visit);
        if (ctx.type() != null) {
            this.visit(ctx.type());
            try {
                symtab.getInnermostActiveScope().define(
                        new ProgVariableSymbol(ctx.name.getText(), ctx,
                                tr.progTypeValues.get(ctx.type()),
                                getRootModuleID()));
            } catch (DuplicateSymbolException e) {
                //This shouldn't be possible--the operation declaration has a
                //scope all its own and we're the first ones to get to
                //introduce anything
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.getStart(), ctx.getText());
            }
        }
        if (ctx.requiresClause() != null) this.visit(ctx.requiresClause());
        if (ctx.ensuresClause() != null) this.visit(ctx.ensuresClause());

        ctx.variableDeclGroup().forEach(this::visit);
        ctx.stmt().forEach(this::visit);

        symtab.endScope();
        currentOpProcedureDecl = null;
        insertFunction(ctx.name, ctx.type(),
                ctx.requiresClause(), ctx.ensuresClause(), ctx);
        return null;
    }

    private void insertFunction(Token name, Resolve.TypeContext type,
            Resolve.RequiresClauseContext requires,
            Resolve.EnsuresClauseContext ensures, ParserRuleContext ctx) {
        try {
            List<ProgParameterSymbol> params =
                    symtab.scopes.get(ctx).getSymbolsOfType(
                            ProgParameterSymbol.class);
            PTType returnType;
            if ( type == null ) {
                returnType = PTVoid.getInstance(g);
            }
            else {
                returnType = tr.progTypeValues.get(type);
            }
            symtab.getInnermostActiveScope().define(
                    new OperationSymbol(name.getText(), ctx, requires, ensures,
                            returnType, getRootModuleID(), params,
                            walkingModuleArgOrParamList));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
    }

    @Override public Void visitParameterDeclGroup(
            Resolve.ParameterDeclGroupContext ctx) {
        this.visit(ctx.type());
        PTType groupType = tr.progTypeValues.get(ctx.type());
        for (TerminalNode term : ctx.ID()) {
            try {
                ProgParameterSymbol.ParameterMode mode =
                        ProgParameterSymbol.getModeMapping().get(
                                ctx.parameterMode().getText());
                symtab.getInnermostActiveScope().define(
                        new ProgParameterSymbol(symtab.getTypeGraph(), term
                                .getText(), mode, groupType, ctx,
                                getRootModuleID()));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        term.getSymbol(), term.getText());
            }
        }
        return null;
    }

    @Override public Void visitFacilityDecl(Resolve.FacilityDeclContext ctx) {
        //Todo: visit the generic arg types too
        ctx.moduleArgumentList().forEach(this::visit);
        ParseTreeProperty<List<ProgTypeSymbol>> facOrEnhToGenericArgs =
                new ParseTreeProperty<>();
        try {
            //map the base facility to any generic symbols parameterizing it
            facOrEnhToGenericArgs.put(ctx,
                    getGenericArgumentSymsForFacilityOrEnh(ctx.type()));

            //now do the same for each enhancement pair
            for (Resolve.EnhancementPairDeclContext enh : ctx.enhancementPairDecl()) {
                //Todo: visit the generic arg types too
                enh.moduleArgumentList().forEach(this::visit);
                facOrEnhToGenericArgs.put(enh,
                        getGenericArgumentSymsForFacilityOrEnh(enh.type()));
            }
            symtab.getInnermostActiveScope().define(
                    new FacilitySymbol(ctx, getRootModuleID(),
                            facOrEnhToGenericArgs, symtab));
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
        return null;
    }

    private List<ProgTypeSymbol> getGenericArgumentSymsForFacilityOrEnh(
            List<Resolve.TypeContext> actualTypes) {
        List<ProgTypeSymbol> result = new ArrayList<>();
        for (Resolve.TypeContext generic : actualTypes) {
            try {
                result.add(symtab.getInnermostActiveScope()
                        .queryForOne(new NameQuery(generic.qualifier,
                                generic.name, true)).toProgTypeSymbol());
            }
            catch (DuplicateSymbolException | NoSuchSymbolException e) {
                compiler.errMgr.semanticError(e.getErrorKind(), generic.name,
                        generic.name.getText());
            }
            catch (UnexpectedSymbolException use) {
                compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                        generic.getStart(), "a program type", generic.getText(),
                        use.getActualSymbolDescription());
            }
        }
        return result;
    }

    @Override public Void visitType(Resolve.TypeContext ctx) {
        try {
            ProgTypeSymbol type =
                    symtab.getInnermostActiveScope()
                            .queryForOne(
                                    new NameQuery(ctx.qualifier, ctx.name, true))
                            .toProgTypeSymbol();
            tr.progTypeValues.put(ctx, type.getProgramType());
            tr.mathTypes.put(ctx, g.MTYPE);
            tr.mathTypeValues.put(ctx, type.getModelType());
            return null;
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(),
                    ctx.getStart(), ctx.name.getText());
        }
        catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "a type", ctx.name.getText(),
                    use.getActualSymbolDescription());
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.progTypeValues.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
        tr.mathTypeValues.put(ctx, MTInvalid.getInstance(g));
        return null;
    }

    @Override public Void visitTypeRepresentationDecl(
            Resolve.TypeRepresentationDeclContext ctx) {
        symtab.startScope(ctx);
        ProgTypeModelSymbol typeDefnSym = null;
        ParseTree reprTypeNode = ctx.type() != null ? ctx.type() : ctx.record();
        this.visit(reprTypeNode);

        try {
            typeDefnSym = symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(null, ctx.name,
                                            false)).toProgTypeModelSymbol();
        }
        catch (NoSuchSymbolException nsse) {
            //this is actually ok for now. Facility module bound type reprs
            //won't have a model.
        }
        catch (UnexpectedSymbolException use) {
            //this is ok too, type representations appearing in facilities wont
            //have a model
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name, ctx.name.getText());
        }

        PTRepresentation reprType =
                new PTRepresentation(g, tr.progTypeValues.get(reprTypeNode),
                        ctx.name.getText(), typeDefnSym, getRootModuleID());
        try {
            String exemplarName = typeDefnSym != null ?
                    typeDefnSym.getExemplar().getName() : ctx.name.getText()
                    .substring(0, 1).toUpperCase();
            symtab.getInnermostActiveScope().define(new ProgVariableSymbol(
                    exemplarName, ctx, reprType, getRootModuleID()));
        }
        catch (DuplicateSymbolException dse) {
            //This shouldn't be possible--the type declaration has a
            //scope all its own and we're the first ones to get to
            //introduce anything
            throw new RuntimeException(dse);
        }
        if (ctx.conventionClause() != null) this.visit(ctx.conventionClause());
        if (ctx.correspondenceClause() != null) this.visit(ctx.correspondenceClause());
        if (ctx.typeImplInit() != null) this.visit(ctx.typeImplInit());
        symtab.endScope();
        PExp convention = getPExpFor(ctx.conventionClause());
        PExp correspondence = getPExpFor(ctx.correspondenceClause());
        try {
            ProgReprTypeSymbol rep = new ProgReprTypeSymbol(g,
                    ctx.name.getText(), ctx, getRootModuleID(),
                    typeDefnSym, reprType, convention, correspondence);
            reprType.setReprTypeSymbol(rep);
            symtab.getInnermostActiveScope().define(rep);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name, ctx.name.getText());
        }

        return null;
    }

    @Override public Void visitRecord(Resolve.RecordContext ctx) {
        Map<String, PTType> fields = new LinkedHashMap<>();
        for (Resolve.RecordVariableDeclGroupContext fieldGrp : ctx
                .recordVariableDeclGroup()) {
            this.visit(fieldGrp);
            PTType grpType = tr.progTypeValues.get(fieldGrp.type());
            for (TerminalNode t : fieldGrp.ID()) {
                fields.put(t.getText(), grpType);
            }
        }
        PTRecord record = new PTRecord(g, fields);
        tr.progTypeValues.put(ctx, record);
        tr.mathTypes.put(ctx, g.MTYPE);
        tr.mathTypeValues.put(ctx, record.toMath());
        return null;
    }

    @Override public Void visitVariableDeclGroup(
            Resolve.VariableDeclGroupContext ctx) {
        this.visit(ctx.type());
        insertVariables(ctx, ctx.ID(), ctx.type());
        return null;
    }

    @Override public Void visitMathTheoremDecl(
            Resolve.MathTheoremDeclContext ctx) {
        symtab.startScope(ctx);
        this.visit(ctx.mathAssertionExp());
        symtab.endScope();
        checkMathTypes(ctx.mathAssertionExp(), g.BOOLEAN);
        try {
            PExp assertion = getPExpFor(ctx.mathAssertionExp());
            symtab.getInnermostActiveScope().define(
                    new TheoremSymbol(g, ctx.name.getText(), assertion,
                            ctx, getRootModuleID()));
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name, ctx.name.getText());
        }
        emit("new theorem: " + ctx.name.getText());
        return null;
    }


    @Override public Void visitMathCategoricalDefinitionDecl(
            Resolve.MathCategoricalDefinitionDeclContext ctx) {
        for (Resolve.MathDefinitionSigContext sig : ctx.mathDefinitionSig()) {
            symtab.startScope(sig);
            this.visit(sig);
            symtab.endScope();

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
        this.visit(ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitMathInductiveDefinitionDecl(
            Resolve.MathInductiveDefinitionDeclContext ctx) {
        symtab.startScope(ctx);
        Resolve.MathDefinitionSigContext sig = ctx.mathDefinitionSig();
        ParserRuleContext baseCase = ctx.mathAssertionExp(0);
        ParserRuleContext indHypo = ctx.mathAssertionExp(1);
        currentInductionVar = ctx.mathVariableDecl();

        activeQuantifications.push(Quantification.UNIVERSAL);
        walkingDefParams = true;
        this.visit(ctx.mathVariableDecl());
        walkingDefParams = false;
        activeQuantifications.pop();

        this.visit(sig);
        this.visit(baseCase);
        this.visit(indHypo);

        MTType defnType = tr.mathTypes.get(ctx.mathDefinitionSig());
        checkMathTypes(baseCase, g.BOOLEAN);
        checkMathTypes(indHypo, g.BOOLEAN);
        symtab.endScope();
        try {
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, sig.name.getText(),
                            defnType, null, ctx, getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    sig.name.getStart(), sig.name.getText());
        }
        currentInductionVar = null;
        definitionSchematicTypes.clear();
        return null;
    }

    private void checkMathTypes(ParserRuleContext ctx, MTType expected) {
        MTType foundType = tr.mathTypes.get(ctx);
        if (!foundType.equals(expected)) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_TYPE,
                    ctx.getStart(), expected, foundType);
        }
    }

    @Override public Void visitMathDefinesDefinitionDecl(
            Resolve.MathDefinesDefinitionDeclContext ctx) {
        this.visit(ctx.mathTypeExp());
        MTType type = tr.mathTypes.get(ctx.mathTypeExp());
        MTType typeValue = tr.mathTypeValues.get(ctx.mathTypeExp());
        for (TerminalNode t : ctx.ID()) {
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, t.getSymbol().getText(),
                                definitionSchematicTypes, type, typeValue,
                                ctx, getRootModuleID()));
            }
            catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
        return null;
    }
    /**
     * Note: Shouldn't be calling this.visit(sigature.xxxx) anywhere at this
     * level. Those visits should all be taken care of in the visitor method
     * for the signature itself.
     */
    @Override public Void visitMathDefinitionDecl(
            Resolve.MathDefinitionDeclContext ctx) {
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
                            definitionSchematicTypes, defnType, defnTypeValue,
                            ctx, getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    sig.name.getStart(), sig.name.getText());
        }
        definitionSchematicTypes.clear();
        return null;
    }

    /**
     * Since 'MathDefinitionSig' appears all over the place within our three
     * styles of definitions (categorical, standard, and inductive), we simply
     * use this signature visitor method to visit and type all relevant
     * children. This way the top level definition nodes can simply grab the type
     * of the signature and build/populate the appropriate object. However,
     * know that in the defn top level nodes, we must remember to start scope,
     * visit the signature, and end scope. We don't do this in the signature
     * because certain information (i.e. body) is rightfully not present.
     * <p>
     * Note also that here we also add a binding for the name of this
     * sig to the active scope (so inductive and implicit definitions may
     * reference themselves).</p>
     */
    @Override public Void visitMathDefinitionSig(
            Resolve.MathDefinitionSigContext ctx) {
        //first visit the formal params
        activeQuantifications.push(Quantification.UNIVERSAL);
        walkingDefParams = true;
        ctx.mathDefinitionParameter().forEach(this::visit);
        walkingDefParams = false;
        activeQuantifications.pop();

        //next, visit the definitions 'return type' to give it a type
        this.visit(ctx.mathTypeExp());

        //finally, build the full type of this definitions signature
        //If there are no params, then it is just the sym after the ':'
        MTType defnType = tr.mathTypeValues.get(ctx.mathTypeExp());
        MTFunction.MTFunctionBuilder builder =
                new MTFunction.MTFunctionBuilder(g, defnType);

        //if there ARE params, then our type needs to be an MTFunction.
        //this if check needs to be here or else, even if there were no params,
        //our type would end up MTFunction: Void -> T (which we don't want)
        if ( !ctx.mathDefinitionParameter().isEmpty() ) {
            for (Resolve.MathDefinitionParameterContext p :
                    ctx.mathDefinitionParameter()) {
                if (p.mathVariableDeclGroup() != null) {
                    MTType grpType = tr.mathTypeValues.get(
                            p.mathVariableDeclGroup().mathTypeExp());
                    for (TerminalNode t : p.mathVariableDeclGroup().ID()) {
                        builder.paramTypes(grpType);
                        builder.paramNames(t.getText());
                    }
                }
                if (p.ID() != null) {
                        //Todo: Get rid of this global eventually and simply
                        //query here for this guy (you need to add him to scope though).
                        if (currentInductionVar == null) {
                            throw new RuntimeException("induction variable missing!?");
                        }
                        MTType inductionVarType =
                                tr.mathTypeValues.get(currentInductionVar.mathTypeExp());
                        builder.paramTypes(inductionVarType)
                                .paramNames(currentInductionVar.ID().getText());
                    //The induction var itself should've already been visited in
                    //visitMathInductiveDefnDecl
                    }
                //if the definition has parameters then it's type should be an
                //MTFunction (e.g. something like a * b ... -> ...)
                defnType = builder.build();
            }
        }
        try {
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, ctx.name.getText(),
                            defnType, null, ctx, getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name.getStart(), ctx.name.getText());
        }
        tr.mathTypes.put(ctx, defnType);
        return null;
    }

    @Override public Void visitMathDefinitionParameter(
            Resolve.MathDefinitionParameterContext ctx) {
        visitChildren(ctx);
        return null;
    }

    @Override public Void visitMathVariableDecl(
            Resolve.MathVariableDeclContext ctx) {
        insertMathVariables(ctx, ctx.mathTypeExp(), ctx.ID());
        return null;
    }

    @Override public Void visitMathVariableDeclGroup(
            Resolve.MathVariableDeclGroupContext ctx) {
        insertMathVariables(ctx, ctx.ID(), ctx.mathTypeExp());
        return null;
    }

    private void insertMathVariables(ParserRuleContext ctx,
             Resolve.MathTypeExpContext type, TerminalNode ... terms) {
        insertMathVariables(ctx, Arrays.asList(terms), type);
    }

    private void insertMathVariables(ParserRuleContext ctx,
            List<TerminalNode> terms, Resolve.MathTypeExpContext type) {
        for (TerminalNode term : terms) {
            this.visit(type);
            MTType mathTypeValue = tr.mathTypeValues.get(type);
            if ( walkingDefParams
                    && mathTypeValue.isKnownToContainOnlyMTypes() ) {
                definitionSchematicTypes.put(term.getText(), mathTypeValue);
            }
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, term.getText(), activeQuantifications
                                .peek(), mathTypeValue, null, ctx,
                                getRootModuleID()));
            }
            catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        term.getSymbol(), term.getText());
            }
        }
    }

    private void insertVariables(ParserRuleContext ctx,
                 List<TerminalNode> terminalGroup, Resolve.TypeContext type) {
        PTType progType = tr.progTypeValues.get(type);
        for (TerminalNode t : terminalGroup) {
            try {
                ProgVariableSymbol vs =
                        new ProgVariableSymbol(t.getText(), ctx, progType,
                                getRootModuleID());
                symtab.getInnermostActiveScope().define(vs);
            }
            catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    @Override public Void visitRequiresClause(
            Resolve.RequiresClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        if ( ctx.entailsClause() != null ) this.visit(ctx.entailsClause());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        if ( ctx.getParent().getParent() instanceof Resolve.ModuleContext ) {
            insertGlobalAssertion(ctx, ctx.mathAssertionExp());
        }
        return null;
    }

    @Override public Void visitEntailsClause(Resolve.EntailsClauseContext ctx) {
        this.visitChildren(ctx);    //Todo : For now.
        return null;
    }

    @Override public Void visitEnsuresClause(Resolve.EnsuresClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitConstraintClause(
            Resolve.ConstraintClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        if ( ctx.getParent().getParent() instanceof Resolve.ModuleContext ) {
            insertGlobalAssertion(ctx, ctx.mathAssertionExp());
        }
        return null;
    }

    @Override public Void visitCorrespondenceClause(
            Resolve.CorrespondenceClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        return null;
    }

    //---------------------------------------------------
    // P R O G    E X P    T Y P I N G
    //---------------------------------------------------

    @Override public Void visitProgNestedExp(Resolve.ProgNestedExpContext ctx) {
        this.visit(ctx.progExp());
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progExp()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progExp()));
        return null;
    }

    @Override public Void visitProgPrimaryExp(
            Resolve.ProgPrimaryExpContext ctx) {
        this.visit(ctx.progPrimary());
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progPrimary()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progPrimary()));
        return null;
    }

    @Override public Void visitProgPrimary(Resolve.ProgPrimaryContext ctx) {
        this.visit(ctx.getChild(0));
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.getChild(0)));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.getChild(0)));
        return null;
    }

    @Override public Void visitProgVarExp(Resolve.ProgVarExpContext ctx) {
        this.visit(ctx.getChild(0));
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.getChild(0)));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.getChild(0)));
        return null;
    }

    @Override public Void visitProgNamedExp(Resolve.ProgNamedExpContext ctx) {
        try {
            ProgVariableSymbol variable =
                    symtab.getInnermostActiveScope().queryForOne(
                            new ProgVariableQuery(ctx.qualifier, ctx.name,
                                    false));
            tr.progTypes.put(ctx, variable.getProgramType());
            exitMathSymbolExp(ctx, ctx.qualifier, ctx.name.getText());
            return null;
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.name,
                    ctx.name.getText());
        }
        catch (UnexpectedSymbolException use) {
            //ok, maybe we're dealing with a reference to an operation then?
            if (walkingModuleArgOrParamList) {
                Symbol s = null;
                try {
                    s = symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(ctx.qualifier,
                                    ctx.name, true));
                    if (s instanceof ModuleParameterizableSymbol) {
                        tr.progTypes.put(ctx, ((ModuleParameterizableSymbol) s).getProgramType());
                        tr.mathTypes.put(ctx, ((ModuleParameterizableSymbol) s).getMathType());
                        return null;
                    }
                } catch (NoSuchSymbolException|DuplicateSymbolException e) {
                    compiler.errMgr.semanticError(e.getErrorKind(), ctx.name,
                            ctx.name.getText());
                }
            }
            //nope. not expecting this at all if we got here. Let's indicate this
            //and type the thing as invalid.
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.name, "a variable reference", ctx.name.getText(),
                    use.getActualSymbolDescription());
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
        return null;
    }

    @Override public Void visitModuleArgument(
            Resolve.ModuleArgumentContext ctx) {
        this.visit(ctx.progExp());
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progExp()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progExp()));
        return null;
    }

    @Override public Void visitModuleArgumentList(
            Resolve.ModuleArgumentListContext ctx) {
        walkingModuleArgOrParamList = true;
        this.visitChildren(ctx);
        walkingModuleArgOrParamList = false;
        return null;
    }

    @Override public Void visitProgMemberExp(Resolve.ProgMemberExpContext ctx) {
        ParseTree firstRecordRef = ctx.getChild(0);
        this.visit(firstRecordRef);
        PTType first = tr.progTypes.get(firstRecordRef);

        //start by checking the first to ensure we're dealing with a record
        if ( !first.isAggregateType() ) {
            compiler.errMgr.semanticError(
                    ErrorKind.ILLEGAL_MEMBER_ACCESS, ctx.getStart(),
                    ctx.getText(), ctx.getChild(0).getText());
            tr.progTypes.put(ctx, PTInvalid.getInstance(g));
            tr.mathTypes.put(ctx, g.INVALID);
            return null;
        }
        PTRepresentation curAggregateType = (PTRepresentation) first;

        //note this will represent the rightmost field type when finished.
        PTType curFieldType = curAggregateType;

        //now we need to make sure our mem accesses aren't nonsense.
        for (TerminalNode term : ctx.ID()) {
            PTRecord recordType = (PTRecord) curAggregateType.getBaseType();
            curFieldType = recordType.getFieldType(term.getText());
            if ( curFieldType == null ) {
                compiler.errMgr.semanticError(ErrorKind.NO_SUCH_SYMBOL,
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
        return null;
    }

    @Override public Void visitProgInfixExp(
            Resolve.ProgInfixExpContext ctx) {
        ctx.progExp().forEach(this::visit);
        List<PTType> argTypes = ctx.progExp().stream()
                .map(tr.progTypes::get).collect(Collectors.toList());
        HardCodedProgOps.BuiltInOpAttributes attr =
                HardCodedProgOps.convert(ctx.op, argTypes);
        typeOperationSym(ctx, attr.qualifier, attr.name, ctx.progExp());
        return null;
    }

    @Override public Void visitProgUnaryExp(Resolve.ProgUnaryExpContext ctx) {
        this.visit(ctx.progExp());
        HardCodedProgOps.BuiltInOpAttributes attr =
                HardCodedProgOps.convert(ctx.op, tr.progTypes.get(ctx.progExp()));
        typeOperationSym(ctx, attr.qualifier, attr.name, ctx.progExp());
        return null;
    }

    @Override public Void visitProgPostfixExp(Resolve.ProgPostfixExpContext ctx) {
        this.visit(ctx.progExp());
        HardCodedProgOps.BuiltInOpAttributes attr =
                HardCodedProgOps.convert(ctx.op, tr.progTypes.get(ctx.progExp()));
        typeOperationSym(ctx, attr.qualifier, attr.name, ctx.progExp());
        return null;
    }

    @Override public Void visitProgParamExp(Resolve.ProgParamExpContext ctx) {
        ctx.progExp().forEach(this::visit);
        typeOperationSym(ctx, ctx.qualifier, ctx.name, ctx.progExp());
        return null;
    }

    @Override public Void visitProgBooleanLiteralExp(
            Resolve.ProgBooleanLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Boolean_Fac", "Boolean");
    }

    @Override public Void visitProgIntegerLiteralExp(
            Resolve.ProgIntegerLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Integer_Fac", "Integer");
    }

    @Override public Void visitProgCharacterLiteralExp(
            Resolve.ProgCharacterLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Character_Fac", "Character");
    }

    @Override public Void visitProgStringLiteralExp(
            Resolve.ProgStringLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Char_Str_Fac", "Char_Str");
    }

    private Void typeProgLiteralExp(ParserRuleContext ctx,
                                    String typeQualifier, String typeName) {
        ProgTypeSymbol p =
                getProgTypeSymbol(ctx, typeQualifier, typeName);
        tr.progTypes.put(ctx, p != null ? p.getProgramType() :
                PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, p != null ? p.getModelType() :
                MTInvalid.getInstance(g));
        return null;
    }

    private ProgTypeSymbol getProgTypeSymbol(ParserRuleContext ctx,
            String typeQualifier, String typeName) {
        CommonToken qualifierToken = new CommonToken(ctx.getStart());
        qualifierToken.setText(typeQualifier);
        qualifierToken.setType(ResolveLexer.ID);

        CommonToken nameToken = new CommonToken(ctx.getStart());
        nameToken.setText(typeName);
        nameToken.setType(ResolveLexer.ID);

        ProgTypeSymbol result = null;
        try {
            result = symtab.getInnermostActiveScope().queryForOne(
                            new NameQuery(qualifierToken, nameToken,false))
                            .toProgTypeSymbol();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(),
                    ctx.getStart(), typeName);
        }
        return result;
    }

    protected void typeOperationSym(ParserRuleContext ctx,
                                    Token qualifier, Token name,
                                    Resolve.ProgExpContext ... args) {
        typeOperationSym(ctx, qualifier, name, Arrays.asList(args));
    }

    protected void typeOperationSym(ParserRuleContext ctx,
                                    Token qualifier, Token name,
                                    List<Resolve.ProgExpContext> args) {
        List<PTType> argTypes = args.stream().map(tr.progTypes::get)
                .collect(Collectors.toList());
        if (currentOpProcedureDecl != null && currentOpProcedureDecl.name
                .getText().equals(name.getText()) ) {
            PTType t = tr.progTypes.get(currentOpProcedureDecl.type()) != null ?
                    tr.progTypes.get(currentOpProcedureDecl.type()) :
                    PTVoid.getInstance(g);
            tr.progTypes.put(ctx, t);
            tr.mathTypes.put(ctx, t.toMath());
            return;
        }
        try {
            OperationSymbol opSym = symtab.getInnermostActiveScope().queryForOne(
                    new OperationQuery(qualifier, name, argTypes,
                            SymbolTable.FacilityStrategy.FACILITY_INSTANTIATE,
                            SymbolTable.ImportStrategy.IMPORT_NAMED));

            tr.progTypes.put(ctx, opSym.getReturnType());
            tr.mathTypes.put(ctx, opSym.getReturnType().toMath());
            return;
        }
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            List<String> argStrList = args.stream()
                    .map(Resolve.ProgExpContext::getText)
                    .collect(Collectors.toList());
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_OPERATION,
                    ctx.getStart(), name.getText(), argStrList, argTypes);
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
    }

    //---------------------------------------------------
    //  M A T H   E X P   T Y P I N G
    //---------------------------------------------------

    @Override public Void visitMathTypeExp(Resolve.MathTypeExpContext ctx) {
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

    @Override public Void visitMathCrossTypeExp(
            Resolve.MathCrossTypeExpContext ctx) {
        typeValueDepth++;
        ctx.mathVariableDeclGroup().forEach(this::visit);

        List<MTCartesian.Element> fieldTypes = new ArrayList<>();
        for (Resolve.MathVariableDeclGroupContext grp : ctx
                .mathVariableDeclGroup()) {
            MTType grpType = tr.mathTypeValues.get(grp.mathTypeExp());
            for (TerminalNode t : grp.ID()) {
                fieldTypes.add(new MTCartesian.Element(t.getText(), grpType));
            }
        }
        tr.mathTypes.put(ctx, g.MTYPE);
        tr.mathTypeValues.put(ctx, new MTCartesian(g, fieldTypes));
        typeValueDepth--;
        return null;
    }

    @Override public Void visitMathNestedExp(Resolve.MathNestedExpContext ctx) {
        this.visit(ctx.mathAssertionExp());
        chainMathTypes(ctx, ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitMathTypeAssertionExp(
            Resolve.MathTypeAssertionExpContext ctx) {
        if (typeValueDepth == 0) {
            this.visit(ctx.mathExp());
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

                //Don't forget to set the type for the var on the lhs of ':'!
                //Todo: Don't know a better way of getting the bottommost rulectx.
                //maybe write a utils method for that? Or read more about the api.
                ParseTree x =
                        ctx.mathExp().getChild(0).getChild(0);
                tr.mathTypes.put(x, assertedType);

                definitionSchematicTypes.put(ctx.mathExp().getText(),
                        tr.mathTypes.get(ctx.mathTypeExp()));
                emit("Added schematic variable: "
                        + ctx.mathExp().getText());
            }
            catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.mathExp().getStart(), ctx.mathExp().getText());
            }
        }
        return null;
    }

    @Override public Void visitMathSetCollectionExp(
            Resolve.MathSetCollectionExpContext ctx) {
        tr.mathTypes.put(ctx, g.SSET);
        ctx.mathExp().forEach(this::visit);
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

            /*if (!chainedTypes.isKnownToContainOnlyMTypes() ||
                    ctx.mathExp().isEmpty()) {
                compiler.errMgr
                        .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                ctx.getStart(), ctx.getText());
                tr.mathTypeValues.put(ctx, g.INVALID);
                return null;
            }*/
            tr.mathTypeValues.put(ctx, chainedTypes);
        }
        return null;
    }

    @Override public Void visitMathFunctionRestrictionExp(
            Resolve.MathFunctionRestrictionExpContext ctx) {
        throw new UnsupportedOperationException("no function restriction " +
                "support yet (even though it's in the grammar)");
    }
    
    @Override public Void visitMathSegmentsExp(
            Resolve.MathSegmentsExpContext ctx) {
        Iterator<Resolve.MathFunctionApplicationExpContext> segsIter =
                ctx.mathFunctionApplicationExp().iterator();
        Resolve.MathFunctionApplicationExpContext nextSeg, lastSeg = null;
        nextSeg = segsIter.next();
        MTType curType = null;
        this.visit(nextSeg);
        if (nextSeg.getText().equals("conc")) {
            nextSeg = segsIter.next();
            this.visit(nextSeg);    //type conc.
            try {
                ProgVariableSymbol programmaticExemplar =
                        symtab.getInnermostActiveScope().queryForOne(
                                new ProgVariableQuery(null, nextSeg.getStart(),
                                        false));
                PTRepresentation repr =
                        ((PTRepresentation) programmaticExemplar
                                .toProgVariableSymbol()
                                .getProgramType());
                try {
                    curType = repr.getFamily().getModelType();
                }
                catch (NoneProvidedException e) {
                    //if a model was not provided to us, then we're a locally defined
                    //type representation and should not be referring to conceptual
                    //variables (because there are none in this case).
                    //Todo: give a better, more official error for this.
                    e.printStackTrace();
                }
            }
            catch (NoSuchSymbolException | DuplicateSymbolException e) {
                e.printStackTrace();
            }
        }
        else {
            curType = tr.mathTypes.get(nextSeg);
        }

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
                    compiler.errMgr.semanticError(
                            ErrorKind.VALUE_NOT_TUPLE, nextSeg.getStart(),
                            segmentName);
                    curType = g.INVALID;
                }
            }
            catch (NoSuchElementException nsee) {
                curType = HardCoded.getMetaFieldType(g, segmentName);
                if ( curType == null ) {
                    compiler.errMgr.semanticError(
                            ErrorKind.NO_SUCH_FACTOR, nextSeg.getStart(),
                            segmentName);
                    curType = g.INVALID;
                }
            }
            tr.mathTypes.put(nextSeg, curType);
            if (nextSeg instanceof Resolve.MathFunctionExpContext) {
                ((Resolve.MathFunctionExpContext)nextSeg).mathExp()
                        .forEach(this::visit);
            }
        }
        tr.mathTypes.put(ctx, curType);
        return null;
    }

    @Override public Void visitMathAssertionExp(
            Resolve.MathAssertionExpContext ctx) {
        this.visit(ctx.getChild(0));
        chainMathTypes(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathPrimeExp(Resolve.MathPrimeExpContext ctx) {
        this.visit(ctx.mathPrimaryExp());
        chainMathTypes(ctx, ctx.mathPrimaryExp());
        return null;
    }

    @Override public Void visitMathPrimaryExp(
            Resolve.MathPrimaryExpContext ctx) {
        this.visit(ctx.getChild(0));
        chainMathTypes(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathLambdaExp(Resolve.MathLambdaExpContext ctx) {
        symtab.startScope(ctx);
        emit("lambda exp: " + ctx.getText());

        walkingDefParams = true;
        activeQuantifications.push(Quantification.UNIVERSAL);
        ctx.mathVariableDeclGroup().forEach(this::visit);
        activeQuantifications.pop();
        walkingDefParams = false;

        this.visit(ctx.mathExp());
        symtab.endScope();

        List<MTType> parameterTypes = new LinkedList<>();
        for (Resolve.MathVariableDeclGroupContext grp :
                ctx.mathVariableDeclGroup()) {
            MTType grpType = tr.mathTypeValues.get(grp.mathTypeExp());
            parameterTypes.addAll(grp.ID().stream()
                    .map(term -> grpType).collect(Collectors.toList()));
        }
        tr.mathTypes.put(ctx, new MTFunction.MTFunctionBuilder(g, tr.mathTypes
                .get(ctx.mathExp())).paramTypes(parameterTypes).build());
        return null;
    }

    @Override public Void visitMathAlternativeExp(
            Resolve.MathAlternativeExpContext ctx) {

        MTType establishedType = null;
        MTType establishedTypeValue = null;
        for (Resolve.MathAlternativeItemExpContext alt : ctx
                .mathAlternativeItemExp()) {
            this.visit(alt.result);
            if (alt.condition != null) this.visit(alt.condition);
            if ( establishedType == null ) {
                establishedType = tr.mathTypes.get(alt.result);
                establishedTypeValue = tr.mathTypeValues.get(alt.result);
            }
            else {
                if ( alt.condition != null ) {
                    // expectType(alt, establishedType);
                }
            }
        }
        tr.mathTypes.put(ctx, establishedType);
        tr.mathTypeValues.put(ctx, establishedTypeValue);
        return null;
    }

    @Override public Void visitMathAlternativeItemExp(
            Resolve.MathAlternativeItemExpContext ctx) {
        if ( ctx.condition != null ) {
            //expectType(ctx.condition, g.BOOLEAN);
        }
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.result));
        tr.mathTypeValues.put(ctx, tr.mathTypeValues.get(ctx.result));
        return null;
    }

    @Override public Void visitMathQuantifiedExp(
            Resolve.MathQuantifiedExpContext ctx) {
        emit("entering mathQuantifiedExp...");
        symtab.startScope(ctx);
        Quantification quantification;

        switch (ctx.q.getType()) {
            case ResolveLexer.FORALL:
                quantification = Quantification.UNIVERSAL;
                break;
            case ResolveLexer.EXISTS:
                quantification = Quantification.EXISTENTIAL;
                break;
            default:
                throw new RuntimeException("unrecognized quantification type: "
                        + ctx.q.getText());
        }
        activeQuantifications.push(quantification);
        this.visit(ctx.mathVariableDeclGroup());
        activeQuantifications.pop();

        activeQuantifications.push(Quantification.NONE);
        this.visit(ctx.mathAssertionExp());
        activeQuantifications.pop();
        emit("exiting mathQuantifiedExp.");
        symtab.endScope();
        tr.mathTypes.put(ctx, g.BOOLEAN);
        return null;
    }

    @Override public Void visitMathUnaryExp(Resolve.MathUnaryExpContext ctx) {
        this.visit(ctx.mathExp());
        typeMathFunctionLikeThing(ctx, null, ctx.op, ctx.mathExp());
        return null;
    }

    @Override public Void visitMathInfixExp(Resolve.MathInfixExpContext ctx) {
        ctx.mathExp().forEach(this::visit);
        typeMathFunctionLikeThing(ctx, null, ctx.op, ctx.mathExp());
        return null;
    }

    @Override public Void visitMathOutfixExp(Resolve.MathOutfixExpContext ctx) {
        this.visit(ctx.mathExp());
        typeMathFunctionLikeThing(ctx, null, new CommonToken(ResolveLexer.ID,
                ctx.lop.getText()+ "..."+ctx.rop.getText()), ctx.mathExp());
        return null;
    }

    @Override public Void visitMathFunctionExp(
            Resolve.MathFunctionExpContext ctx) {
        ctx.mathExp().forEach(this::visit);
        typeMathFunctionLikeThing(ctx, null, ctx.name, ctx.mathExp());
        return null;
    }

    @Override public Void visitMathBooleanLiteralExp(
            Resolve.MathBooleanLiteralExpContext ctx) {
        exitMathSymbolExp(ctx, null, ctx.getText());
        return null;
    }

    @Override public Void visitMathIntegerLiteralExp(
            Resolve.MathIntegerLiteralExpContext ctx) {
        exitMathSymbolExp(ctx, ctx.qualifier, ctx.num.getText());
        return null;
    }

    @Override public Void visitMathVariableExp(
            Resolve.MathVariableExpContext ctx) {
        exitMathSymbolExp(ctx, ctx.qualifier, ctx.name.getText());
        return null;
    }

    private MathSymbol exitMathSymbolExp(ParserRuleContext ctx, Token qualifier,
                                         String symbolName) {
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
                                MathSymbol intendedEntry) {
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
                if (!moduleScope.getDependentTerms().contains(symbolName)) {
                    compiler.errMgr
                            .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                    ctx.getStart(), symbolName);
                }
                tr.mathTypeValues.put(ctx, g.INVALID);
            }
        }
    }

    private void typeMathFunctionLikeThing(ParserRuleContext ctx,
                               Token qualifier, Token name,
                               Resolve.MathExpContext... args) {
        typeMathFunctionLikeThing(ctx, qualifier, name, Arrays.asList(args));
    }

    private void typeMathFunctionLikeThing(ParserRuleContext ctx,
                               Token qualifier, Token name,
                               List<Resolve.MathExpContext> args) {
        String foundExp = ctx.getText();
        MTFunction foundExpType;
        foundExpType =
                PSymbol.getConservativePreApplicationType(g, args, tr.mathTypes);

        emit("expression: " + ctx.getText() + "("
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
                if (moduleScope.getDependentTerms().contains(arg.getText())) {
                    argTypeValue = new MTNamed(g, arg.getText());
                }
                else if ( argTypeValue == null ) {
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

    private MathSymbol getIntendedFunction(ParserRuleContext ctx,
                               Token qualifier, Token name,
                               List<Resolve.MathExpContext> args) {
        tr.mathTypes.put(ctx, PSymbol.getConservativePreApplicationType(g,
                args, tr.mathTypes));
        PSymbol e = (PSymbol)getPExpFor(ctx);

        MTFunction eType = (MTFunction)e.getMathType();

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

        emit("matching " + name.getText() + " : " + eType
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
                String originalCandidateType = candidate.getType().toString();
                candidate =
                        candidate.deschematize(e.getArguments(),
                                definitionSchematicTypes,
                                symtab.getInnermostActiveScope());
                candidateType = (MTFunction) candidate.getType();
                emit(originalCandidateType + " deschematizes to "
                        + candidateType);

                if ( comparison.compare(e, eType, candidateType) ) {
                    if ( match != null ) {
                       /* compiler.errMgr.semanticError(
                                ErrorKind.AMBIGIOUS_DOMAIN,null, match
                                        .getName(), match.getType(), candidate
                                        .getName(), candidate.getType());*/
                        return match;
                    }
                    match = candidate;
                }
            }
            catch (NoSolutionException nse) {
                //couldn't deschematize--try the next one
                emit(candidate.getType() + " doesn't deschematize "
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
            return true;
        }

        @Override public String description() {
            return "inexact";
        }
    }

    private void insertGlobalAssertion(ParserRuleContext ctx,
                            Resolve.MathAssertionExpContext assertion) {
        String name = ctx.getText() + "_" + globalSpecCount++;
        PExp assertionAsPExp = getPExpFor(assertion);
        try {
            symtab.getInnermostActiveScope().define(
                    new GlobalMathAssertionSymbol(name, assertionAsPExp, ctx,
                            getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.getText());
        }
    }

    protected final PExp getPExpFor(ParseTree ctx) {
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

    private void chainProgTypes(ParseTree current, ParseTree child) {
        tr.progTypes.put(current, tr.progTypes.get(child));
        tr.progTypeValues.put(current, tr.progTypeValues.get(child));
    }

    private String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }

    private void emit(String msg) {
        if (EMIT_DEBUG) {
            emit(msg);
        }
    }
}
