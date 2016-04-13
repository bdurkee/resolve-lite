package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.*;
import org.rsrg.semantics.MathCartesianClassification.Element;
import org.rsrg.semantics.programtype.*;
import org.rsrg.semantics.query.MathSymbolQuery;
import org.rsrg.semantics.query.NameQuery;
import org.rsrg.semantics.symbol.*;

import java.util.*;

public class PopulatingVisitor extends ResolveBaseVisitor<Void> {

    private ModuleScopeBuilder moduleScope = null;

    private final RESOLVECompiler compiler;
    private final MathSymbolTable symtab;

    private final AnnotatedModule tr;
    private final DumbTypeGraph g;

    /** While walking children of an
     *      {@link ResolveParser.MathCategoricalDefnDeclContext} or
     *      {@link ResolveParser.MathStandardDefnDeclContext} or
     *      {@link ResolveParser.MathInductiveDefnDeclContext}
     *  (namely, one of the four styles of defn signatures therein), this
     *  holds a ref to the scope that the defn binding should be added to;
     *  holds {@code null} otherwise.
     */
    private Scope defnEnclosingScope = null;

    /** This is {@code true} if and only if we're visiting  ctxs on the right
     *  hand side of a colon (<tt>:</tt>); {@code false} otherwise.
     */
    private boolean walkingType = false;
    private boolean walkingDefnParams = false;

    private final ParseTreeProperty<List<ProgTypeSymbol>>
            actualGenericTypesPerFacilitySpecArgs = new ParseTreeProperty<>();
    /** A mapping from {@code ParserRuleContext}s to their corresponding
     *  {@link MathClassification}s; only applies to exps.
     */
    public ParseTreeProperty<MathClassification> exactNamedMathClssftns =
            new ParseTreeProperty<>();

    /** A reference to the expr context that represents the previous segment
     *  accessed in a {@link ResolveParser.MathSelectorExpContext} or
     *  {@link ResolveParser.ProgSelectorExpContext}, no need to worry about
     *  overlap here as we use two separate expr hierarchies. This is
     *  {@code null} the rest of the time.
     */
    private ParserRuleContext prevSelectorAccess = null;

    /** Holds a ref to a type model symbol while walking it (or its repr). */
    private TypeModelSymbol curTypeReprModelSymbol = null;

    public PopulatingVisitor(@NotNull RESOLVECompiler rc,
                             @NotNull MathSymbolTable symtab,
                             @NotNull AnnotatedModule annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tr = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    public DumbTypeGraph getTypeGraph() {
        return g;
    }

    @Override public Void visitModuleDecl(ResolveParser.ModuleDeclContext ctx) {
        moduleScope = symtab.startModuleScope(tr)
                .addImports(tr.semanticallyRelevantUses);
        super.visitChildren(ctx);
        symtab.endScope();
        return null; //java requires a return, even if its 'Void'
    }

    @Override public Void visitPrecisModuleDecl(
            ResolveParser.PrecisModuleDeclContext ctx) {
        if (ctx.tag != null) {
            symtab.addTag(new ModuleIdentifier(ctx.tag));
        }
        super.visitChildren(ctx);
        return null;
    }

    @Override public Void visitPrecisExtModuleDecl(
            ResolveParser.PrecisExtModuleDeclContext ctx) {
        try {
            //exts implicitly gain the parenting precis's useslist
            ModuleScopeBuilder conceptScope = symtab.getModuleScope(
                    new ModuleIdentifier(ctx.precis));
            moduleScope.addImports(conceptScope.getImports());
            moduleScope.addInheritedModules(new ModuleIdentifier(ctx.precis));
        } catch (NoSuchModuleException e) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                    ctx.precis, ctx.precis.getText());
        }
        super.visitChildren(ctx);
        return null;
    }

    @Override public Void visitConceptImplModuleDecl(
            ResolveParser.ConceptImplModuleDeclContext ctx) {
        try {
            //implementations implicitly gain the parenting concept's useslist
            ModuleScopeBuilder conceptScope = symtab.getModuleScope(
                    new ModuleIdentifier(ctx.concept));
            moduleScope.addImports(conceptScope.getImports());

            moduleScope.addInheritedModules(new ModuleIdentifier(ctx.concept));
        } catch (NoSuchModuleException e) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                    ctx.concept, ctx.concept.getText());
        }
        super.visitChildren(ctx);
        return null;
    }

    @Override public Void visitParameterDeclGroup(
            ResolveParser.ParameterDeclGroupContext ctx) {
        this.visit(ctx.type());
        ProgType groupType = tr.progTypes.get(ctx.type());

        for (TerminalNode term : ctx.ID()) {
            try {
                ProgParameterSymbol.ParameterMode mode =
                        ProgParameterSymbol.getModeMapping().get(
                                ctx.parameterMode().getText());
                ProgParameterSymbol p =
                        new ProgParameterSymbol(symtab.getTypeGraph(), term
                                .getText(), mode, groupType,
                                ctx, getRootModuleIdentifier());
                if (ctx.type() instanceof ResolveParser.NamedTypeContext) {
                    ResolveParser.NamedTypeContext asNamedType =
                            (ResolveParser.NamedTypeContext)ctx.type();
                    p.setTypeQualifierString(asNamedType.qualifier == null ? null :
                            asNamedType.qualifier.getText());
                }
                boolean walkingModuleParamList = Utils.getFirstAncestorOfType(
                        ctx, ResolveParser.SpecModuleParameterListContext.class,
                        ResolveParser.ImplModuleParameterListContext.class) != null;
                if (walkingModuleParamList) {
                    symtab.getInnermostActiveScope()
                            .define(new ModuleParameterSymbol(p));
                }
                else {
                    symtab.getInnermostActiveScope()
                            .define(p);
                }
            } catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        term.getSymbol(), term.getText());
            }
        }
        return null;
    }

    @Override public Void visitFacilityDecl(
            ResolveParser.FacilityDeclContext ctx) {
        initializeAndSanityCheckInfo(ctx);
        //now visit all supplied actual arg exprs
        ctx.moduleArgumentList().forEach(this::visit);
        for (ResolveParser.ExtensionPairingContext extension :
                ctx.extensionPairing()) {
            extension.moduleArgumentList().forEach(this::visit);
        }
        try {
            //before we even construct the facility we ensure things like
            //formal counts and actual counts (also for generics) is the same
            FacilitySymbol facility = new FacilitySymbol(ctx,
                    getRootModuleIdentifier(),
                    actualGenericTypesPerFacilitySpecArgs, symtab);
            symtab.getInnermostActiveScope().define(facility);
            //we got some checking to do now..
            // facility.getFacility().getSpecification().getArguments()
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
        return null;
    }

    @Override public Void visitOperationDecl(
            ResolveParser.OperationDeclContext ctx) {
        symtab.startScope(ctx);
        ctx.operationParameterList().parameterDeclGroup().forEach(this::visit);
        if (ctx.type() != null) {
            this.visit(ctx.type());
            try {
                symtab.getInnermostActiveScope().addBinding(ctx.name.getText(),
                        ctx.getParent(), exactNamedMathClssftns.get(ctx.type()));
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
            ResolveParser.OperationProcedureDeclContext ctx) {
        symtab.startScope(ctx);
        ctx.operationParameterList().parameterDeclGroup().forEach(this::visit);
        if (ctx.type() != null) {
            this.visit(ctx.type());
            try {
                symtab.getInnermostActiveScope().define(
                        new ProgVariableSymbol(ctx.name.getText(), ctx,
                                tr.progTypes.get(ctx.type()),
                                getRootModuleIdentifier()));
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

        ctx.varDeclGroup().forEach(this::visit);
        //ctx.stmt().forEach(this::visit);
        //sanityCheckStmtsForReturn(ctx.name, ctx.type(), ctx.stmt());

        symtab.endScope();
        insertFunction(ctx.name, ctx.type(),
                ctx.requiresClause(), ctx.ensuresClause(), ctx);
        return null;
    }

    private void insertFunction(@NotNull Token name,
                                @Nullable ResolveParser.TypeContext type,
                                @Nullable ResolveParser.RequiresClauseContext requires,
                                @Nullable ResolveParser.EnsuresClauseContext ensures,
                                @NotNull ParserRuleContext ctx) {
        try {
            List<ProgParameterSymbol> params =
                    symtab.getScope(ctx).getSymbolsOfType(
                            ProgParameterSymbol.class);
            ProgType returnType;
            if (type == null) {
                returnType = ProgVoidType.getInstance(g);
            } else {
                returnType = tr.progTypes.get(type);
            }

            PExp requiresExp = getPExpFor(requires);
           PExp ensuresExp = getPExpFor(ensures);
            //TODO: this will need to be wrapped in a ModuleParameterSymbol
            //if we're walking a specmodule param list
            symtab.getInnermostActiveScope().define(
                    new OperationSymbol(name.getText(), ctx, requiresExp,
                            ensuresExp, returnType, getRootModuleIdentifier(),
                            params));
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
    }

    /** Really just checks two things before we add an {@link FacilitySymbol}
     * to the table:
     *  1. That the number of actuals supplied to module {@code i}
     *     matches the number of formals.
     *  2. The number prog types (or even generics) supplied matches the number
     *     of formal type parameters.
     */
    private void sanityCheckParameterizationArgs(
            @NotNull List<ResolveParser.ProgExpContext> actuals,
            @NotNull ModuleIdentifier i) {
        List<ProgType> argTypes = new ArrayList<>();
        try {
            ModuleScopeBuilder module = symtab.getModuleScope(i);
            List<ModuleParameterSymbol> formals =
                    module.getSymbolsOfType(ModuleParameterSymbol.class);
            for (ResolveParser.ProgExpContext arg : actuals) {
                argTypes.add(tr.progTypes.get(arg));
            }
            if (argTypes.size() != formals.size()) {
                //ERROR
            }
            //now make sure the top level type params (at least) match up...
            Iterator<ProgType> actualTypesIter = argTypes.iterator();
            Iterator<ModuleParameterSymbol> formalParamIter = formals.iterator();
            /*while (actualTypesIter.hasNext()) {
                PTType actualType = actualTypesIter.next();
                ModuleParameterSymbol formalParam = formalParamIter.next();
                if (formalParam.getWrappedParamSymbol() instanceof MathSymbol) continue;
                //unbelievable, we need the actual Symbols for the actual args...
                //if (formalParam.isModuleTypeParameter() && actualType.)
                //if (actualType.isTypeLike() && formalParam.getProgramType().isTypeLike())
            }*/
        } catch (NoSuchModuleException e) {
            e.printStackTrace();
        }
    }

    private void initializeAndSanityCheckInfo(
            @NotNull ResolveParser.FacilityDeclContext ctx) {

        if (ctx.specArgs != null) {
            sanityCheckParameterizationArgs(ctx.specArgs.progExp(),
                    new ModuleIdentifier(ctx.spec));
            actualGenericTypesPerFacilitySpecArgs
                    .put(ctx.specArgs, new ArrayList<>());
        }
        if (ctx.implArgs != null) {
            sanityCheckParameterizationArgs(ctx.implArgs.progExp(),
                    new ModuleIdentifier(ctx.impl));
        }
        for (ResolveParser.ExtensionPairingContext extension :
                ctx.extensionPairing()) {
            if (extension.specArgs != null) {
                actualGenericTypesPerFacilitySpecArgs.put(extension.specArgs, new ArrayList<>());
            }
        }
    }

    @Override public Void visitNamedType(ResolveParser.NamedTypeContext ctx) {
        try {
            Token qualifier = ctx.qualifier;
            ProgTypeSymbol type =
                    symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(qualifier, ctx.name, true))
                            .toProgTypeSymbol();

            tr.progTypes.put(ctx, type.getProgramType());
            tr.mathClssftns.put(ctx, type.getModelType());
            return null;
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(),
                    ctx.name.getText());
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "a type", ctx.name.getText(),
                    use.getTheUnexpectedSymbolDescription());
        } catch (NoSuchModuleException nsme) {
            noSuchModule(nsme);
        }
        tr.progTypes.put(ctx, ProgInvalidType.getInstance(g));
        tr.mathClssftns.put(ctx, g.INVALID);
        return null;
    }

    @Override public Void visitTypeModelDecl(
            ResolveParser.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
        this.visit(ctx.mathClssftnExp());
        MathSymbol exemplarSymbol = null;
        MathClassification modelType =
                exactNamedMathClssftns.get(ctx.mathClssftnExp());
        MathNamedClassification exemplarMathType =
                new MathNamedClassification(g, ctx.exemplar.getText(),
                modelType.getTypeRefDepth() - 1,
                modelType);
        try {

            exemplarSymbol =
                    symtab.getInnermostActiveScope().addBinding(
                            ctx.exemplar.getText(), ctx,
                            //give the exemplar symbol a value for itself.
                            exemplarMathType);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.getText());
        }
        if (ctx.constraintsClause() != null) this.visit(ctx.constraintsClause());
        if (ctx.initializationClause() != null) this.visit(ctx.initializationClause());
        symtab.endScope();
        try {
            PExp constraint = getPExpFor(ctx.constraintsClause());
            PExp initEnsures = getPExpFor(ctx.initializationClause());

            ProgTypeSymbol progType =
                    new TypeModelSymbol(symtab.getTypeGraph(),
                            ctx.name.getText(), modelType,
                            new ProgFamilyType(modelType, ctx.name.getText(),
                                    ctx.exemplar.getText(), g.getTrueExp(),
                                    g.getTrueExp(), getRootModuleIdentifier()),
                            exemplarSymbol, ctx, getRootModuleIdentifier());
            symtab.getInnermostActiveScope().define(progType);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
        return null;
    }

    @Override public Void visitTypeRepresentationDecl(
            ResolveParser.TypeRepresentationDeclContext ctx) {
        symtab.startScope(ctx);
        ParseTree reprTypeNode = ctx.type();
        this.visit(reprTypeNode);

        try {
            curTypeReprModelSymbol = symtab.getInnermostActiveScope()
                    .queryForOne(new NameQuery(null, ctx.name,
                            false)).toTypeModelSymbol();
        } catch (NoSuchSymbolException | UnexpectedSymbolException nsse) {
            //this is actually ok for now. Facility module bound type reprs
            //won't have a model.
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name, ctx.name.getText());
        } catch (NoSuchModuleException nsme) {
            compiler.errMgr.semanticError(nsme.getErrorKind(),
                    nsme.getRequestedModule(),
                    nsme.getRequestedModule().getText());
        }

        //need to implement visitprogrecordtype
        PTRepresentation reprType =
                new PTRepresentation(g, tr.progTypes.get(reprTypeNode),
                        ctx.name.getText(), curTypeReprModelSymbol,
                        getRootModuleIdentifier());
        try {
            String exemplarName = curTypeReprModelSymbol != null ?
                    curTypeReprModelSymbol.getExemplar().getName() :
                    ctx.name.getText().substring(0, 1).toUpperCase();
            symtab.getInnermostActiveScope().define(new ProgVariableSymbol(
                    exemplarName, ctx, reprType, getRootModuleIdentifier()));

        } catch (DuplicateSymbolException dse) {
            //This shouldn't be possible--the type declaration has a
            //scope all its own and we're the first ones to get to
            //introduce anything
            throw new RuntimeException(dse);
        }
        PExp convention = g.getTrueExp();
        PExp correspondence = g.getTrueExp();
        if (ctx.conventionsClause() != null) {
            this.visit(ctx.conventionsClause());
            convention = getPExpFor(ctx.conventionsClause().mathAssertionExp());
        }
        if (ctx.correspondenceClause() != null) {
            this.visit(ctx.correspondenceClause());
            correspondence = getPExpFor(ctx.correspondenceClause().mathAssertionExp());
        }
        if (ctx.typeImplInit() != null) this.visit(ctx.typeImplInit());
        symtab.endScope();
        try {
            ProgReprTypeSymbol rep = new ProgReprTypeSymbol(g,
                    ctx.name.getText(), ctx, getRootModuleIdentifier(),
                    curTypeReprModelSymbol, reprType, convention,
                    correspondence);
            reprType.setReprTypeSymbol(rep);
            symtab.getInnermostActiveScope().define(rep);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name, ctx.name.getText());
        }
        curTypeReprModelSymbol = null;
        return null;
    }

    @Override public Void visitRecordType(ResolveParser.RecordTypeContext ctx) {
        Map<String, ProgType> fields = new LinkedHashMap<>();
        for (ResolveParser.RecordVarDeclGroupContext fieldGrp : ctx
                .recordVarDeclGroup()) {
            this.visit(fieldGrp);
            ProgType grpType = tr.progTypes.get(fieldGrp.type());
            for (TerminalNode t : fieldGrp.ID()) {
                fields.put(t.getText(), grpType);
            }
        }
        ProgRecordType record = new ProgRecordType(g, fields);
        tr.mathClssftns.put(ctx, record.toMath());
        tr.progTypes.put(ctx, record);
        return null;
    }

    @Override public Void visitGenericTypeParameterDecl(
            ResolveParser.GenericTypeParameterDeclContext ctx) {
        try {
            //all generic params are module params; its the only way they can
            //be introduced.
            ModuleParameterSymbol moduleParam =
                    new ModuleParameterSymbol(new ProgParameterSymbol(g,
                            ctx.name.getText(),
                            ProgParameterSymbol.ParameterMode.TYPE,
                            new ProgGenericType(g, ctx.name.getText()),
                            ctx, getRootModuleIdentifier()));
            symtab.getInnermostActiveScope().define(moduleParam);
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.ID().getText());
        }
        return null;
    }

    @Override public Void visitVarDeclGroup(
            ResolveParser.VarDeclGroupContext ctx) {
        this.visit(ctx.type());
        insertVariables(ctx, ctx.ID(), ctx.type());
        return null;
    }

    private void insertVariables(@NotNull ParserRuleContext ctx,
                                 @NotNull List<TerminalNode> terminalGroup,
                                 @NotNull ResolveParser.TypeContext type) {
        ProgType progType = tr.progTypes.get(type);
        for (TerminalNode t : terminalGroup) {
            try {
                ProgVariableSymbol vs =
                        new ProgVariableSymbol(t.getText(), ctx, progType,
                                getRootModuleIdentifier());
                symtab.getInnermostActiveScope().define(vs);
            }
            catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    // math constructs

    @Override public Void visitMathClassificationTheoremDecl(
            ResolveParser.MathClassificationTheoremDeclContext ctx) {
        ctx.mathExp().forEach(this::visit);
        MathClassification x = exactNamedMathClssftns.get(ctx.mathExp(0));
        MathClassification y = exactNamedMathClssftns.get(ctx.mathExp(1));
        g.relationships.put(x, y);
        return null;
    }

    @Override public Void visitMathTheoremDecl(
            ResolveParser.MathTheoremDeclContext ctx) {
        symtab.startScope(ctx);
        this.visit(ctx.mathAssertionExp());
        symtab.endScope();
        MathClassification x = tr.mathClssftns.get(ctx.mathAssertionExp());
        expectType(ctx.mathAssertionExp(), g.BOOLEAN);
        try {
            //PExp assertion = getMathExpASTFor(ctx.mathAssertionExp());
            symtab.getInnermostActiveScope().define(
                    new TheoremSymbol(g, ctx.name.getText(), g.getTrueExp(),
                            ctx, getRootModuleIdentifier()));
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name, ctx.name.getText());
        }
        return null;
    }

    @Override public Void visitMathCategoricalDefnDecl(
            ResolveParser.MathCategoricalDefnDeclContext ctx) {
        for (ResolveParser.MathPrefixDefnSigContext sig :
                ctx.mathPrefixDefnSigs().mathPrefixDefnSig()) {
            defnEnclosingScope = symtab.getInnermostActiveScope();
            symtab.startScope(ctx);
            this.visit(sig);
            symtab.endScope();
            defnEnclosingScope = null;
        }
        //visit the predicate that groups together the components of our
        //categorical defn
        this.visit(ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitMathInductiveDefnDecl(
            ResolveParser.MathInductiveDefnDeclContext ctx) {
        defnEnclosingScope = symtab.getInnermostActiveScope();
        symtab.startScope(ctx);
        ResolveParser.MathDefnSigContext sig = ctx.mathDefnSig();
        ParserRuleContext baseCase = ctx.mathAssertionExp(0);
        ParserRuleContext indHypo = ctx.mathAssertionExp(1);

        //note that 'sig' adds a binding for the name to the active scope
        //so baseCase and indHypo will indeed be able to see the symbol we're
        //introducing here.
        this.visit(sig);
        this.visit(baseCase);
        this.visit(indHypo);

        expectType(baseCase, g.BOOLEAN);
        expectType(indHypo, g.BOOLEAN);
        symtab.endScope();
        defnEnclosingScope = null;
        return null;
    }

    @Override public Void visitMathStandardDefnDecl(
            ResolveParser.MathStandardDefnDeclContext ctx) {
        defnEnclosingScope = symtab.getInnermostActiveScope();
        symtab.startScope(ctx);
        this.visit(ctx.mathDefnSig());
        if (ctx.body != null) this.visit(ctx.body);
        symtab.endScope();
        defnEnclosingScope = null;
        return null;
    }

    @Override public Void visitMathDefnSig(
            ResolveParser.MathDefnSigContext ctx) {
        this.visitChildren(ctx);
        return null;
    }

    @Override public Void visitMathInfixDefnSig(
            ResolveParser.MathInfixDefnSigContext ctx) {
        try {
            insertMathDefnSignature(ctx, ctx.mathVarDecl(), ctx.mathClssftnExp(),
                    ctx.name.getStart());
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), e.getOffendingSymbol().getName());
        }
        return null;
    }

    @Override public Void visitMathPrefixDefnSig(
            ResolveParser.MathPrefixDefnSigContext ctx) {
        try {
            insertMathDefnSignature(ctx, ctx.mathVarDeclGroup(),
                    ctx.mathClssftnExp(),
                    Utils.apply(ctx.mathSymbolName(),
                            ParserRuleContext::getStart));
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), e.getOffendingSymbol().getName());
        }
        return null;
    }

    @Override public Void visitMathPostfixDefnSig(
            ResolveParser.MathPostfixDefnSigContext ctx) {
        try {
            CommonToken name = new CommonToken(ctx.lop);
            name.setText(ctx.lop.getText()+".."+ctx.rop.getText());
            insertMathDefnSignature(ctx, ctx.mathVarDecl(),
                    ctx.mathClssftnExp(), name);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), e.getOffendingSymbol().getName());
        }
        return null;
    }

    private void insertMathDefnSignature(@NotNull ParserRuleContext ctx,
                                         @NotNull List<? extends ParseTree> formals,
                                         @NotNull ResolveParser.MathClssftnExpContext type,
                                         @NotNull Token ... names)
            throws DuplicateSymbolException {
        insertMathDefnSignature(ctx, formals, type, Arrays.asList(names));
    }

    private void insertMathDefnSignature(@NotNull ParserRuleContext ctx,
                                         @NotNull List<? extends ParseTree> formals,
                                         @NotNull ResolveParser.MathClssftnExpContext type,
                                         @NotNull List<? extends Token> names)
            throws DuplicateSymbolException {
        //first visit the formal params
        walkingDefnParams = true;
        formals.forEach(this::visit);
        walkingDefnParams = false;

        //next, visit the definition's 'return type' to give it a type
        this.visit(type);
        MathClassification colonRhsType =
                exactNamedMathClssftns.get(type);

        MathClassification defnType = null;
        if (colonRhsType.typeRefDepth > 0) {
            int newTypeDepth = colonRhsType.typeRefDepth - 1;
            List<MathClassification> paramTypes = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();

            if (!formals.isEmpty()) {
                for (ParseTree formal : formals) {
                    try {
                        ResolveParser.MathVarDeclGroupContext grp =
                                (ResolveParser.MathVarDeclGroupContext) formal;
                        for (TerminalNode t : grp.ID()) {
                            MathClassification ty =
                                    exactNamedMathClssftns.get(grp.mathClssftnExp());
                            paramTypes.add(ty);
                            paramNames.add(t.getText());
                        }
                    }
                    catch (ClassCastException cce) {
                        ResolveParser.MathVarDeclContext singularDecl =
                                (ResolveParser.MathVarDeclContext) formal;
                            MathClassification ty = exactNamedMathClssftns.get(
                                    singularDecl.mathClssftnExp());
                            paramTypes.add(ty);
                            paramNames.add(singularDecl.ID().getText());
                    }
                }
                defnType = new MathFunctionClassification(
                        g, colonRhsType, paramNames, paramTypes);

                for (Token t : names) {
                    MathClassification asNamed = new MathNamedClassification(g, t.getText(),
                            newTypeDepth, defnType);
                    defnEnclosingScope
                            .define(new MathSymbol(g, t.getText(), asNamed));
                }
            } else {
                for (Token t : names) {
                    defnType = new MathNamedClassification(g, t.getText(),
                            newTypeDepth, colonRhsType);
                    defnEnclosingScope
                            .define(new MathSymbol(g, t.getText(), defnType));
                }
            }
        } else {
            for (Token t : names) {
                defnEnclosingScope
                        .define(new MathSymbol(g, t.getText(), g.INVALID));
            }
        }
    }

    @Override public Void visitMathVarDeclGroup(
            ResolveParser.MathVarDeclGroupContext ctx) {
        insertMathVarDecls(ctx, ctx.mathClssftnExp(), ctx.ID());
        return null;
    }

    @Override public Void visitMathVarDecl(
            ResolveParser.MathVarDeclContext ctx) {
        insertMathVarDecls(ctx, ctx.mathClssftnExp(), ctx.ID());
        return null;
    }

    private void insertMathVarDecls(@NotNull ParserRuleContext ctx,
                                    @NotNull ResolveParser.MathClssftnExpContext t,
                                    @NotNull TerminalNode... terms) {
        insertMathVarDecls(ctx, t, Arrays.asList(terms));
    }

    private void insertMathVarDecls(@NotNull ParserRuleContext ctx,
                                    @NotNull ResolveParser.MathClssftnExpContext t,
                                    @NotNull List<TerminalNode> terms) {
        String x = ctx.getText();
        this.visitMathClssftnExp(t);
        MathClassification rhsColonType = exactNamedMathClssftns.get(t);
        for (TerminalNode term : terms) {
            MathClassification ty = new MathNamedClassification(g, term.getText(),
                    rhsColonType.typeRefDepth - 1, rhsColonType);

            ty.identifiesSchematicType = walkingDefnParams &&
                    (rhsColonType == g.SSET || rhsColonType == g.CLS ||
                    rhsColonType instanceof MathPowersetApplicationClassification);
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, term.getText(), ty));
            } catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.getStart(), e.getOffendingSymbol().getName());
            }
        }
    }

    @Override public Void visitRequiresClause(
            ResolveParser.RequiresClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        this.visit(ctx.entailsClause());
        return null;
    }

    private MathClassification entailsRetype = null;
    @Override public Void visitEntailsClause(
            ResolveParser.EntailsClauseContext ctx) {
        for (ResolveParser.EntailsClssftnsContext clfsGrp :
                ctx.entailsClssftns()) {
            this.visit(clfsGrp.mathClssftnExp());
            entailsRetype = exactNamedMathClssftns.get(clfsGrp.mathClssftnExp());
            clfsGrp.mathExp().forEach(this::visit);
            entailsRetype = null;
        }
        return null;
    }

    @Override public Void visitMathClssftnExp(
            ResolveParser.MathClssftnExpContext ctx) {
        walkingType = true;
        this.visit(ctx.mathExp());
        walkingType = false;

        MathClassification type = exactNamedMathClssftns.get(ctx.mathExp());
        if (type == g.INVALID || type == null || type.getTypeRefDepth() == 0 ) {
            compiler.errMgr.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
            type = g.INVALID;
        }
        exactNamedMathClssftns.put(ctx, type);
        tr.mathClssftns.put(ctx, type.enclosingClassification);
        return null;
    }

    /*@Override public Void visitMathEntailsExp(
            ResolveParser.MathEntailsExpContext ctx) {
        //this.visit(ctx.m)
        return null;
    }*/

    //boolean walkingEntailsClause = Utils.getFirstAncestorOfType(
    //        ctx, ResolveParser.EntailsClauseContext.class) != null;
    @Override public Void visitMathClssftnAssertionExp(
            ResolveParser.MathClssftnAssertionExpContext ctx) {
        this.visit(ctx.mathExp());
        MathClassification rhsColonType =
                exactNamedMathClssftns.get(ctx.mathExp());
        boolean walkingEntails =
                Utils.getFirstAncestorOfType(ctx,
                        ResolveParser.EntailsClauseContext.class) != null;

        if (walkingEntails) {

        }

        ty.identifiesSchematicType = true;
        try {
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, ctx.ID().getText(), ty));
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), e.getOffendingSymbol().getName());
        }

        //defnSchematicTypes.put(ctx.ID().getText(), ty);
        exactNamedMathClssftns.put(ctx, ty);
        tr.mathClssftns.put(ctx, ty);
        return null;
    }

    @Override public Void visitMathQuantifiedExp(
            ResolveParser.MathQuantifiedExpContext ctx) {
        symtab.startScope(ctx);
        Quantification quantification;

        /*switch (ctx.q.getType()) {
            case ResolveLexer.FORALL:
                quantification = Quantification.UNIVERSAL;
                break;
            case ResolveLexer.EXISTS:
                quantification = Quantification.EXISTENTIAL;
                break;
            default:
                throw new RuntimeException("unrecognized quantification type: "
                        + ctx.q.getText());
        }*/
        //activeQuantifications.push(quantification);
        this.visit(ctx.mathVarDeclGroup());
        //activeQuantifications.pop();

        //activeQuantifications.push(Quantification.NONE);
        this.visit(ctx.mathAssertionExp());
        //activeQuantifications.pop();
        symtab.endScope();
        tr.mathClssftns.put(ctx, g.BOOLEAN);
        return null;
    }

    @Override public Void visitMathAssertionExp(
            ResolveParser.MathAssertionExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathPrimaryExp(
            ResolveParser.MathPrimaryExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.mathPrimeExp());
        return null;
    }

    @Override public Void visitMathPrimeExp(
            ResolveParser.MathPrimeExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathNestedExp(
            ResolveParser.MathNestedExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitMathInfixAppExp(
            ResolveParser.MathInfixAppExpContext ctx) {
        typeMathFunctionAppExp(ctx, (ParserRuleContext) ctx.getChild(1),
                ctx.mathExp());
        return null;
    }

    @Override public Void visitMathPrefixAppExp(
            ResolveParser.MathPrefixAppExpContext ctx) {
        typeMathFunctionAppExp(ctx, ctx.name,
                ctx.mathExp().subList(1, ctx.mathExp().size()));
        return null;
    }

    @Override public Void visitMathBracketAppExp(
            ResolveParser.MathBracketAppExpContext ctx) {
        typeMathFunctionAppExp(ctx, ctx.mathSqBrOpExp(), ctx.mathExp());
        return null;
    }

    //TODO: Outfix. Infix postfix?
    private void typeMathFunctionAppExp(@NotNull ParserRuleContext ctx,
                                        @NotNull ParserRuleContext nameExp,
                                        @NotNull ParseTree... args) {
        typeMathFunctionAppExp(ctx, nameExp, Arrays.asList(args));
    }

    private void typeMathFunctionAppExp(@NotNull ParserRuleContext ctx,
                                        @NotNull ParserRuleContext nameExp,
                                        @NotNull List<? extends ParseTree> args) {
        this.visit(nameExp);

        args.forEach(this::visit);
        String asString = ctx.getText();
        MathClassification t = exactNamedMathClssftns.get(nameExp);
        //if we're a name identifying a function, get our function type.
        if (t instanceof MathNamedClassification &&
                t.getEnclosingClassification() instanceof MathFunctionClassification) {
            t = ((MathNamedClassification) t).enclosingClassification;
        }
        if (!(t instanceof MathFunctionClassification)) {
            compiler.errMgr.semanticError(ErrorKind.APPLYING_NON_FUNCTION,
                    nameExp.getStart(), nameExp.getText());
            exactNamedMathClssftns.put(ctx, g.INVALID);
            tr.mathClssftns.put(ctx, g.INVALID);
            return;
        }
        MathFunctionClassification expectedFuncType =
                (MathFunctionClassification) t;
        List<MathClassification> actualArgumentTypes =
                Utils.apply(args, tr.mathClssftns::get);
        List<MathClassification> formalParameterTypes =
                expectedFuncType.getParamTypes();
        //ugly hook to handle chained operator applications like 0 <= i <= j or
        //j >= i >= 0
        if ((nameExp.getText().equals("<=") || nameExp.getText().equals("<") || nameExp.getText().equals("≤")) &&
                args.size() == 2 &&
                args.get(0) instanceof ResolveParser.MathInfixAppExpContext) {
            ResolveParser.MathInfixAppExpContext argAsInfixApp =
                    (ResolveParser.MathInfixAppExpContext)args.get(0);
            if (argAsInfixApp.getChild(1).getText().equals("<=") ||
                    argAsInfixApp.getChild(1).getText().equals("≤") ||
                    argAsInfixApp.getChild(1).getText().equals("<")) {
                MathClassification x = tr.mathClssftns.get(argAsInfixApp.getChild(2));
                actualArgumentTypes.set(0, x);
            }
        } //end ugly hook.
        //TODO: Factor this out to a helper, get it out of my face.
        if (formalParameterTypes.size() != actualArgumentTypes.size()) {
            compiler.errMgr.semanticError(ErrorKind.INCORRECT_FUNCTION_ARG_COUNT,
                    ctx.getStart(), ctx.getText());
            exactNamedMathClssftns.put(ctx, g.INVALID);
            tr.mathClssftns.put(ctx, g.INVALID);
            return;
        }
        try {
            MathClassification oldExpectedFuncType = expectedFuncType;

            expectedFuncType = (MathFunctionClassification)
                    expectedFuncType.deschematize(actualArgumentTypes);
            if (!oldExpectedFuncType.toString().equals(expectedFuncType.toString())) {
                compiler.errMgr.info("expected function type: "+oldExpectedFuncType);
                compiler.errMgr.info("   deschematizes to: "+expectedFuncType);
            }
        } catch (BindingException e) {
            System.out.println("formal params in: '" + asString +
                    "' don't bind against the actual arg types");
        }
        //we have to redo this since deschematize above might've changed the
        //args
        formalParameterTypes = expectedFuncType.getParamTypes();

        List<MathClassification> actualValues =
                Utils.apply(args, exactNamedMathClssftns::get);
        Iterator<MathClassification> actualsIter = actualArgumentTypes.iterator();
        Iterator<MathClassification> formalsIter = formalParameterTypes.iterator();
        Iterator<MathClassification> actualValuesIter = actualValues.iterator();

        //SUBTYPE AND EQUALITY CHECK FOR ARGS HAPPENS HERE
        while (actualsIter.hasNext()) {
            MathClassification actual = actualsIter.next();
            //MathClassification actualValue = actualValuesIter.next();
            MathClassification formal = formalsIter.next();
            if (!g.isSubtype(actual, formal)) {
                    System.err.println("for function application: " +
                            ctx.getText() + "; arg type: " + actual +
                            " not acceptable where: " + formal + " was expected");
            }
        }

        //If we're describing a type, then the range (as a result of the function is too broad),
        //so we'll annotate the type of this application with its (verbose) application type.
        //but it's enclosing type will of course still be the range.
        if (walkingType && expectedFuncType.getResultType().getTypeRefDepth() <= 1) {
            exactNamedMathClssftns.put(ctx, g.INVALID);
            tr.mathClssftns.put(ctx, g.INVALID);
        }
        else if (walkingType) {
            List<MathClassification> actualNamedArgumentTypes =
                    Utils.apply(args, exactNamedMathClssftns::get);
            MathClassification appType =
                    expectedFuncType.getApplicationType(
                            nameExp.getText(), actualNamedArgumentTypes);
            exactNamedMathClssftns.put(ctx, appType);
            tr.mathClssftns.put(ctx, appType);
        } else {
            //the classification of an f-application exp is the range of f,
            //according to the rule:
            //  C \ f : C x D -> R
            //  C \ E1 : C
            //  C \ E2 : D
            //  ---------------------
            //  C \ f(E1, E2) : R
            exactNamedMathClssftns.put(ctx, expectedFuncType.getResultType());
            tr.mathClssftns.put(ctx, expectedFuncType.getResultType());
        }
    }
    /*
    mathSqBrOpExp : op='[' ;
    mathMultOpExp : (qualifier=ID '::')? op=('*'|'/'|'%') ;
    mathAddOpExp : (qualifier=ID '::')? op=('+'|'-'|'~');
    mathJoiningOpExp : (qualifier=ID '::')? op=('o'|'union'|'∪'|'∪₊'|'intersect'|'∩'|'∩₊');
    mathArrowOpExp : (qualifier=ID '::')? op=('->'|'⟶') ;
    mathRelationalOpExp : (qualifier=ID '::')? op=('<'|'>'|'<='|'≤'|'≤ᵤ'|'>='|'≥');
    mathEqualityOpExp : (qualifier=ID '::')? op=('='|'/='|'≠');
    mathSetContainmentOpExp : (qualifier=ID '::')? op=('is_in'|'is_not_in'|'∈'|'∉');
    mathImpliesOpExp : (qualifier=ID '::')? op='implies';
    mathBooleanOpExp : (qualifier=ID '::')? op=('and'|'or'|'iff');
    */
    @Override public Void visitMathSqBrOpExp(
            ResolveParser.MathSqBrOpExpContext ctx) {
        typeMathSymbol(ctx, null, "[..]");
        return null;
    }
    @Override public Void visitMathMultOpExp(
            ResolveParser.MathMultOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathAddOpExp(
            ResolveParser.MathAddOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathJoiningOpExp(
            ResolveParser.MathJoiningOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathArrowOpExp(
            ResolveParser.MathArrowOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathRelationalOpExp(
            ResolveParser.MathRelationalOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathEqualityOpExp(
            ResolveParser.MathEqualityOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathSetContainmentOpExp(
            ResolveParser.MathSetContainmentOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathImpliesOpExp(
            ResolveParser.MathImpliesOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathBooleanOpExp(
            ResolveParser.MathBooleanOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathBooleanLiteralExp(
            ResolveParser.MathBooleanLiteralExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathIntegerLiteralExp(
            ResolveParser.MathIntegerLiteralExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.INT().getText());
        return null;
    }

    @Override public Void visitMathSymbolExp(
            ResolveParser.MathSymbolExpContext ctx) {
        if (prevSelectorAccess != null) {
            typeMathSelectorAccessExp(ctx, prevSelectorAccess,
                    ctx.name.getText());
        }
        else {
            typeMathSymbol(ctx, ctx.qualifier, ctx.name.getText());
        }
        return null;
    }

    @Override public Void visitMathCartProdExp(
            ResolveParser.MathCartProdExpContext ctx) {
        ctx.mathVarDeclGroup().forEach(this::visit);
        List<MathSymbol> fieldSyms = new ArrayList<>();
        for (ResolveParser.MathVarDeclGroupContext grp :
                ctx.mathVarDeclGroup()) {
            for (TerminalNode t : grp.ID()) {
                fieldSyms.add(getIntendedMathSymbol(null, t.getText(), grp));
            }
        }
        List<Element> fields= new ArrayList<>();
        for (ResolveParser.MathVarDeclGroupContext grp : ctx
                .mathVarDeclGroup()) {
            MathClassification grpType =
                    exactNamedMathClssftns
                            .get(grp.mathClssftnExp());
            for (TerminalNode label : grp.ID()) {
                fields.add(new Element(label.getText(), grpType));
            }
        }
        MathCartesianClassification cartClssftn =
                new MathCartesianClassification(g, fields);
        for (MathSymbol fieldSym : fieldSyms) {
            cartClssftn.syms.put(fieldSym.getName(), fieldSym);
        }
        tr.mathClssftns.put(ctx, new MathCartesianClassification(g, fields));
        exactNamedMathClssftns.put(ctx,
                new MathCartesianClassification(g, fields));
        return null;
    }

    @Override public Void visitMathSetRestrictionExp(
            ResolveParser.MathSetRestrictionExpContext ctx) {
        this.visit(ctx.mathVarDecl());
        this.visit(ctx.mathAssertionExp());
        MathClassification t =
                g.POWERSET_FUNCTION.getApplicationType("Powerset",
                        exactNamedMathClssftns.get(
                                ctx.mathVarDecl().mathClssftnExp()));
        exactNamedMathClssftns.put(ctx, t);
        tr.mathClssftns.put(ctx, t);
        return null;
    }

    @Override public Void visitMathSetExp(ResolveParser.MathSetExpContext ctx) {
        ctx.mathExp().forEach(this::visit);
        if (ctx.mathExp().isEmpty()) {
            tr.mathClssftns.put(ctx, g.EMPTY_SET);
        }
        else {
            MathClassification t =
                    g.POWERSET_FUNCTION.getApplicationType("Powerset",
                            exactNamedMathClssftns
                                    .get(ctx.mathExp(0)));
            tr.mathClssftns.put(ctx, t);
        }
        return null;
    }

    @Override public Void visitMathLambdaExp(
            ResolveParser.MathLambdaExpContext ctx) {
        symtab.startScope(ctx);
        compiler.errMgr.info("lambda exp: " + ctx.getText());

        walkingDefnParams = true;
        //activeQuantifications.push(Quantification.UNIVERSAL);
        this.visit(ctx.mathVarDecl());
        //activeQuantifications.pop();
        walkingDefnParams = false;

        this.visit(ctx.mathExp());
        symtab.endScope();

        MathClassification bodyClss = tr.mathClssftns.get(ctx.mathExp());
        MathClassification varClss = exactNamedMathClssftns.get(
                ctx.mathVarDecl().mathClssftnExp());
        tr.mathClssftns.put(ctx, new MathFunctionClassification(
                g, bodyClss, varClss));
        return null;
    }

    @Override public Void visitMathAlternativeExp(
            ResolveParser.MathAlternativeExpContext ctx) {

        MathClassification establishedType = null;
        MathClassification establishedTypeValue = null;
        for (ResolveParser.MathAlternativeItemExpContext alt : ctx
                .mathAlternativeItemExp()) {
            this.visit(alt.result);
            if (alt.condition != null) this.visit(alt.condition);
            if (establishedType == null) {
                establishedType = tr.mathClssftns.get(alt.result);
            }
            //else {
            //if ( alt.condition != null ) {
            // expectType(alt, establishedType);
            //}
            //}
        }
        tr.mathClssftns.put(ctx, establishedType);
        return null;
    }

    @Override public Void visitMathAlternativeItemExp(
            ResolveParser.MathAlternativeItemExpContext ctx) {
        if ( ctx.condition != null ) {
            expectType(ctx.condition, g.BOOLEAN);
        }
        tr.mathClssftns.put(ctx, tr.mathClssftns.get(ctx.result));
        //tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.result));
        //tr.mathTypeValues.put(ctx, tr.mathTypeValues.get(ctx.result));
        return null;
    }

    @Override public Void visitMathSelectorExp(
            ResolveParser.MathSelectorExpContext ctx) {
        this.visit(ctx.lhs);
        prevSelectorAccess = ctx.lhs;
        this.visit(ctx.rhs);
        prevSelectorAccess = null;

        MathClassification finalClassfctn = tr.mathClssftns.get(ctx.rhs);
        tr.mathClssftns.put(ctx, finalClassfctn);
        exactNamedMathClssftns.put(ctx, finalClassfctn);
        return null;
    }

    private void typeMathSelectorAccessExp(@NotNull ParserRuleContext ctx,
                                           @NotNull ParserRuleContext prevAccessExp,
                                           @NotNull String symbolName) {

        MathClassification type;
        MathClassification prevMathAccessType =
                tr.mathClssftns.get(prevAccessExp);
        //Todo: This can't go into {@link #getMetaFieldType()} since
        //it starts the access chain, rather than, say, terminating it.
        if (prevAccessExp.getText().equals("conc")) {
            if (curTypeReprModelSymbol == null) {
                compiler.errMgr.semanticError(ErrorKind.NO_SUCH_FACTOR,
                        ctx.getStart(), symbolName);
                tr.mathClssftns.put(ctx, g.INVALID); return;
            }
            tr.mathClssftns.put(ctx, curTypeReprModelSymbol.getModelType());
            return;
        }
        try {
            MathCartesianClassification typeCartesian =
                    (MathCartesianClassification) prevMathAccessType;
            if (entailsRetype != null) {
                MathSymbol x = typeCartesian.syms.get(symbolName);
                if (x != null) x.setClassification(entailsRetype);
            }
            type = typeCartesian.getFactor(symbolName);
        }
        catch (ClassCastException|NoSuchElementException cce) {
            type = getMetaFieldType(g, symbolName);
            if (type == null) {
                compiler.errMgr.semanticError(
                        ErrorKind.NO_SUCH_FACTOR, ctx.getStart(),
                        symbolName);
                type = g.INVALID;
            }
        }
        tr.mathClssftns.put(ctx, type);
    }

    private void typeMathSymbol(@NotNull ParserRuleContext ctx,
                                @Nullable Token qualifier,
                                @NotNull String name) {
        String here = ctx.getText();

        MathSymbol s = getIntendedMathSymbol(qualifier, name, ctx);
        if (s == null || s.getClassification() == null) {
            exactNamedMathClssftns.put(ctx, g.INVALID);
            tr.mathClssftns.put(ctx, g.INVALID);
            return;
        }
        if (entailsRetype != null) {
            s.setClassification(
                    new MathNamedClassification(g, name,
                            entailsRetype.typeRefDepth-1, entailsRetype));
        }
        exactNamedMathClssftns.put(ctx, s.getClassification());
        if (s.getClassification().identifiesSchematicType) {
            tr.mathClssftns.put(ctx, s.getClassification());
        }
        else {
            tr.mathClssftns.put(ctx, s.getClassification()
                    .getEnclosingClassification());
        }
    }

    @Nullable private MathSymbol getIntendedMathSymbol(
            @Nullable Token qualifier, @NotNull String symbolName,
            @NotNull ParserRuleContext ctx) {
        try {
            return symtab.getInnermostActiveScope()
                    .queryForOne(new MathSymbolQuery(qualifier,
                            symbolName, ctx.getStart()));
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(),
                    symbolName);
        } catch (NoSuchModuleException nsme) {
            compiler.errMgr.semanticError(nsme.getErrorKind(),
                    nsme.getRequestedModule(),
                    nsme.getRequestedModule().getText());
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "a math symbol", symbolName,
                    use.getTheUnexpectedSymbolDescription());
        }
        return null;
    }

    public static MathClassification getMetaFieldType(
            @NotNull DumbTypeGraph g, @NotNull String metaSegment) {
        MathClassification result = null;

        if ( metaSegment.equals("Is_Initial") ) {
            result = new MathFunctionClassification(g, g.BOOLEAN, g.ENTITY);
        }
        else if ( metaSegment.equals("base_point") ) {
            result = g.ENTITY;
        }
        return result;
    }

    private void expectType(ParserRuleContext ctx, MathClassification expected) {
        MathClassification foundType = tr.mathClssftns.get(ctx);
        if (!g.isSubtype(foundType, expected)) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_TYPE,
                    ctx.getStart(), expected, foundType);
        }
    }

    /** Given some context {@code ctx} and a
     *  {@code child} context; this method visits {@code child} and chains/passes
     *  its found {@link MathClassification} upto {@code ctx}.
     *
     * @param ctx a parent {@code ParseTree}
     * @param child one of {@code ctx}s children
     */
    private void visitAndClassifyMathExpCtx(@NotNull ParseTree ctx,
                                            @NotNull ParseTree child) {
        this.visit(child);
        exactNamedMathClssftns.put(ctx,
                exactNamedMathClssftns.get(child));
        MathClassification x = tr.mathClssftns.get(child);
        tr.mathClssftns.put(ctx, tr.mathClssftns.get(child));
    }

    private PExp getPExpFor(@Nullable ParserRuleContext ctx) {
        if (ctx == null) {
            return g.getTrueExp();
        }
        PExpBuildingListener l = new PExpBuildingListener(g, tr);
        ParseTreeWalker.DEFAULT.walk(l, ctx);
        return l.getBuiltPExp(ctx);
    }

    private ModuleIdentifier getRootModuleIdentifier() {
        return symtab.getInnermostActiveScope().getModuleIdentifier();
    }

    private void noSuchModule(NoSuchModuleException nsme) {
        compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                nsme.getRequestedModule(),
                nsme.getRequestedModule().getText());
    }
}
