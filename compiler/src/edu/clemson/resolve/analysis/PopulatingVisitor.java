package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.misc.StdTemplateProgOps;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PExpBuildingListener;
import edu.clemson.resolve.semantics.*;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol.ParameterMode;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.MathCartesianClssftn.Element;
import edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy;
import edu.clemson.resolve.semantics.programtype.*;
import edu.clemson.resolve.semantics.query.MathSymbolQuery;
import edu.clemson.resolve.semantics.query.NameQuery;
import edu.clemson.resolve.semantics.query.OperationQuery;
import edu.clemson.resolve.semantics.symbol.*;

import java.util.*;
import java.util.stream.Collectors;

public class PopulatingVisitor extends ResolveBaseVisitor<Void> {

    private ModuleScopeBuilder moduleScope = null;

    private final RESOLVECompiler compiler;
    private final MathSymbolTable symtab;

    private final AnnotatedModule tr;
    private final DumbMathClssftnHandler g;

    /**
     * While walking children of an {@link ResolveParser.MathCategoricalDefnDeclContext} or
     * {@link ResolveParser.MathStandardDefnDeclContext} or {@link ResolveParser.MathInductiveDefnDeclContext}
     * (namely, one of the four styles of defn signatures therein), this holds a ref to the {@link Scope} that the defn
     * binding should be added to; holds {@code null} otherwise.
     */
    private Scope defnEnclosingScope = null;

    /**
     * This is {@code true} if and only if we're visiting  ctxs on the right hand side of a colon (<tt>:</tt>);
     * {@code false} otherwise.
     */
    private boolean walkingType = false;
    private boolean walkingDefnParams = false;

    private final ParseTreeProperty<List<ProgTypeSymbol>> actualGenericTypesPerFacilitySpecArgs =
            new ParseTreeProperty<>();
    /**
     * A mapping from {@code ParserRuleContext}s to their corresponding {@link MathClssftn}s;
     * only applies to exps.
     */
    public ParseTreeProperty<MathClssftn> exactNamedMathClssftns = new ParseTreeProperty<>();
    public ParseTreeProperty<String> progExpCallQualifiers = new ParseTreeProperty<>();
    /**
     * A reference to the expr context that represents the previous segment
     * accessed in a {@link ResolveParser.MathSelectorExpContext} or
     * {@link ResolveParser.ProgSelectorExpContext}, no need to worry about
     * overlap here as we use two separate expr hierarchies. This is
     * {@code null} the rest of the time.
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

    public DumbMathClssftnHandler getTypeGraph() {
        return g;
    }

    @Override
    public Void visitModuleDecl(ResolveParser.ModuleDeclContext ctx) {
        moduleScope = symtab.startModuleScope(tr).addImports(tr.uses);
        super.visitChildren(ctx);
        symtab.endScope();
        return null; //java requires a return, even if its 'Void'
    }

    @Override
    public Void visitPrecisModuleDecl(ResolveParser.PrecisModuleDeclContext ctx) {
        super.visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitPrecisExtModuleDecl(ResolveParser.PrecisExtModuleDeclContext ctx) {
      /*  try {
            //exts implicitly gain the parenting precis's useslist
            ModuleScopeBuilder conceptScope = symtab.getModuleScope(new ModuleIdentifier(ctx.precis));
            moduleScope.addImports(conceptScope.getImports());
            moduleScope.addInheritedModules(new ModuleIdentifier(ctx.precis));
        } catch (NoSuchModuleException e) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE, ctx.precis, ctx.precis.getText());
        }*/
        super.visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitConceptImplModuleDecl(ResolveParser.ConceptImplModuleDeclContext ctx) {
    /*    try {
            //implementations implicitly gain the parenting concept's useslist
            ModuleScopeBuilder conceptScope = symtab.getModuleScope(new ModuleIdentifier(ctx.concept));
            moduleScope.addImports(conceptScope.getImports());

            moduleScope.addInheritedModules(new ModuleIdentifier(ctx.concept));
        } catch (NoSuchModuleException e) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE, ctx.concept, ctx.concept.getText());
        }*/
        super.visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitConceptExtModuleDecl(ResolveParser.ConceptExtModuleDeclContext ctx) {
      /*  try {
            //implementations implicitly gain the parenting concept's useslist
            ModuleScopeBuilder conceptScope = symtab.getModuleScope(new ModuleIdentifier(ctx.concept));
            moduleScope.addImports(conceptScope.getImports());
            moduleScope.addInheritedModules(new ModuleIdentifier(ctx.concept));
        } catch (NoSuchModuleException e) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE, ctx.concept, ctx.concept.getText());
        }*/
        super.visitChildren(ctx);
        return null;
    }


    @Override
    public Void visitParameterDeclGroup(ResolveParser.ParameterDeclGroupContext ctx) {
        this.visit(ctx.type());
        ProgType groupType = tr.progTypes.get(ctx.type());

        ParameterMode mode = ProgParameterSymbol.getModeMapping().get(ctx.parameterMode().getText());
        if (mode == ParameterMode.INVALID || mode == null) {
            compiler.errMgr.semanticError(ErrorKind.INVALID_PARAM_MODE, ctx.parameterMode().getStart(),
                    ctx.parameterMode().getText());
            mode = ParameterMode.INVALID;
        }
        for (TerminalNode term : ctx.ID()) {
            try {
                ProgParameterSymbol p =
                        new ProgParameterSymbol(symtab.getTypeGraph(),
                                term.getText(), mode, groupType, ctx, getRootModuleIdentifier());

                if (ctx.type() instanceof ResolveParser.NamedTypeContext) {
                    ResolveParser.NamedTypeContext asNamedType = (ResolveParser.NamedTypeContext) ctx.type();
                    p.setTypeQualifierString(asNamedType.qualifier == null ? null : asNamedType.qualifier.getText());
                }
                boolean walkingModuleParamList =
                        Utils.getFirstAncestorOfType(ctx, ResolveParser.SpecModuleParameterListContext.class,
                                ResolveParser.ImplModuleParameterListContext.class) != null;
                if (walkingModuleParamList) {
                    symtab.getInnermostActiveScope().define(new ModuleParameterSymbol(p));
                }
                else {
                    symtab.getInnermostActiveScope().define(p);
                }
            } catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, term.getSymbol(), term.getText());
            }
        }
        return null;
    }

    @Override
    public Void visitFacilityDecl(ResolveParser.FacilityDeclContext ctx) {
        initializeAndSanityCheckInfo(ctx);
        //now visit all supplied actual arg exprs
        ctx.moduleArgumentList().forEach(this::visit);
        for (ResolveParser.ExtensionPairingContext extension : ctx.extensionPairing()) {
            extension.moduleArgumentList().forEach(this::visit);
        }
        try {
            //these two lines will throw the appropriate exception (that is caught below)
            //if the modules don't exist or aren't imported...
            symtab.getModuleScope(moduleScope.getImportWithName(ctx.spec));

            //before we even construct the facility we ensure things like
            //formal counts and actual counts (also for generics) is the same
            FacilitySymbol facility = new FacilitySymbol(ctx, getRootModuleIdentifier(),
                    actualGenericTypesPerFacilitySpecArgs, symtab);
            symtab.getInnermostActiveScope().define(facility);
            //we got some checking to do now..
            // facility.getFacility().getSpecification().getArguments()
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name, ctx.name.getText());
        } catch (NoSuchModuleException e) {
            noSuchModule(e);
            try {
                if (ctx.externally == null) symtab.getModuleScope(moduleScope.getImportWithName(ctx.impl));
            }
            catch (NoSuchModuleException e2) {
                noSuchModule(e2);
            }
        }
        return null;
    }

    @Override
    public Void visitOperationDecl(ResolveParser.OperationDeclContext ctx) {
        symtab.startScope(ctx);
        ctx.operationParameterList().parameterDeclGroup().forEach(this::visit);
        if (ctx.type() != null) {
            this.visit(ctx.type());
            MathClssftn x = tr.mathClssftns.get(ctx.type());
            try {
                symtab.getInnermostActiveScope().addBinding(ctx.name.getText(),
                        ctx.getParent(), new MathNamedClssftn(
                                g, ctx.name.getText(), x.typeRefDepth - 1, x));
            } catch (DuplicateSymbolException e) {
                //This shouldn't be possible--the operation declaration has a
                //scope all its own and we're the first ones to get to
                //introduce anything
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.getStart(), ctx.getText());
            }
        }
        if (ctx.requiresClause() != null) this.visit(ctx.requiresClause());
        if (ctx.ensuresClause() != null) this.visit(ctx.ensuresClause());
        symtab.endScope();

        insertFunction(ctx.name, ctx.type(), ctx.requiresClause(), ctx.ensuresClause(), ctx);
        return null;
    }

    @Override
    public Void visitOperationProcedureDecl(ResolveParser.OperationProcedureDeclContext ctx) {
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
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.getStart(), ctx.getText());
            }
        }
        if (ctx.requiresClause() != null) this.visit(ctx.requiresClause());
        if (ctx.ensuresClause() != null) this.visit(ctx.ensuresClause());

        ctx.varDeclGroup().forEach(this::visit);
        ctx.stmt().forEach(this::visit);
        sanityCheckStmtsForReturn(ctx.name, ctx.type(), ctx.stmt());

        symtab.endScope();
        insertFunction(ctx.name, ctx.type(), ctx.requiresClause(), ctx.ensuresClause(), ctx);
        return null;
    }

    @Override
    public Void visitProcedureDecl(ResolveParser.ProcedureDeclContext ctx) {
        OperationSymbol correspondingOp = null;
        try {
            correspondingOp =
                    symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(null, ctx.name.getText(), false))
                            .toOperationSymbol();
        } catch (NoSuchSymbolException nse) {
            compiler.errMgr.semanticError(ErrorKind.DANGLING_PROCEDURE, ctx.getStart(), ctx.name.getText());
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.getStart(), ctx.getText());
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL, ctx.name, "an operation",
                    ctx.name.getText(), use.getTheUnexpectedSymbolDescription());
        } catch (NoSuchModuleException nsme) {
            noSuchModule(nsme);
        }
        symtab.startScope(ctx);
        this.visit(ctx.operationParameterList());
        ProgType returnType = null;
        if (ctx.type() != null) {
            this.visit(ctx.type());
            returnType = tr.progTypes.get(ctx.type());
            try {
                symtab.getInnermostActiveScope().define(
                        new ProgVariableSymbol(ctx.name.getText(), ctx, returnType, getRootModuleIdentifier()));
            } catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name, ctx.name.getText());
            }
        }
        else {
            returnType = ProgVoidType.getInstance(g);
        }
        ctx.varDeclGroup().forEach(this::visit);
        ctx.stmt().forEach(this::visit);
        sanityCheckStmtsForReturn(ctx.name, ctx.type(), ctx.stmt());
        symtab.endScope();
        if (correspondingOp == null) { //backout
            return null;
        }
        try {
            symtab.getInnermostActiveScope()
                    .define(new ProcedureSymbol(ctx.name.getText(), ctx,
                            getRootModuleIdentifier(), correspondingOp));
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.getStart(), ctx.name.getText());
        }
        return null;
    }

    private void insertFunction(@NotNull Token name,
                                @Nullable ResolveParser.TypeContext type,
                                @Nullable ResolveParser.RequiresClauseContext requires,
                                @Nullable ResolveParser.EnsuresClauseContext ensures,
                                @NotNull ParserRuleContext ctx) {
        try {
            List<ProgParameterSymbol> params = symtab.getScope(ctx).getSymbolsOfType(ProgParameterSymbol.class);
            ProgType returnType;
            if (type == null) {
                returnType = ProgVoidType.getInstance(g);
            }
            else {
                returnType = tr.progTypes.get(type);
            }
            PExp requiresExp = g.getTrueExp();
            PExp ensuresExp = g.getTrueExp();
            if (requires != null) requiresExp = getPExpFor(requires.mathAssertionExp());
            if (ensures != null) ensuresExp = getPExpFor(ensures.mathAssertionExp());
            //TODO: this will need to be wrapped in a ModuleParameterSymbol
            //if we're walking a specmodule param list
            symtab.getInnermostActiveScope().define(
                    new OperationSymbol(name.getText(), ctx, requiresExp,
                            ensuresExp, returnType, getRootModuleIdentifier(), params));
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, name, name.getText());
        }
    }

    /**
     * Really just checks two things before we add an {@link FacilitySymbol} to the table:
     * <ol>
     * <li>That the number of actuals supplied to module {@code i}
     * matches the number of formals</li>
     * <li>The number prog types (or even generics) supplied matches the number
     * of formal type parameters.</li>
     * </ol>
     */
    private void sanityCheckParameterizationArgs(@NotNull List<ResolveParser.ProgExpContext> actuals,
                                                 @NotNull ModuleIdentifier i) {
        List<ProgType> argTypes = new ArrayList<>();
        try {
            ModuleScopeBuilder module = symtab.getModuleScope(i);
            List<ModuleParameterSymbol> formals = module.getSymbolsOfType(ModuleParameterSymbol.class);
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

    private void initializeAndSanityCheckInfo(@NotNull ResolveParser.FacilityDeclContext ctx) {
/*
        if (ctx.specArgs != null) {
            sanityCheckParameterizationArgs(ctx.specArgs.progExp(), new ModuleIdentifier(ctx.spec));
            actualGenericTypesPerFacilitySpecArgs.put(ctx.specArgs, new ArrayList<>());
        }
        if (ctx.implArgs != null) {
            sanityCheckParameterizationArgs(ctx.implArgs.progExp(), new ModuleIdentifier(ctx.impl));
        }*/
        for (ResolveParser.ExtensionPairingContext extension : ctx.extensionPairing()) {
            if (extension.specArgs != null) {
                actualGenericTypesPerFacilitySpecArgs.put(extension.specArgs, new ArrayList<>());
            }
        }
    }

    @Override
    public Void visitNamedType(ResolveParser.NamedTypeContext ctx) {
        try {
            Token qualifier = ctx.qualifier;
            ProgTypeSymbol type =
                    symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(qualifier, ctx.name.getText(), true))
                            .toProgTypeSymbol();

            tr.progTypes.put(ctx, type.getProgramType());
            tr.mathClssftns.put(ctx, type.getModelType());
            //MathClassification x = type.getModelType();
            return null;
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.name, ctx.name.getText());
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL, ctx.getStart(), "a type",
                    ctx.name.getText(), use.getTheUnexpectedSymbolDescription());
        } catch (NoSuchModuleException nsme) {
            noSuchModule(nsme);
        }
        tr.progTypes.put(ctx, ProgInvalidType.getInstance(g));
        tr.mathClssftns.put(ctx, g.INVALID);
        return null;
    }

    @Override
    public Void visitTypeModelDecl(ResolveParser.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
        this.visit(ctx.mathClssftnExp());
        MathClssftnWrappingSymbol exemplarSymbol = null;
        MathClssftn modelType = exactNamedMathClssftns.get(ctx.mathClssftnExp());
        MathNamedClssftn exemplarMathType =
                new MathNamedClssftn(g, ctx.exemplar.getText(),
                        modelType.getTypeRefDepth() - 1,
                        modelType);
        try {

            exemplarSymbol =
                    symtab.getInnermostActiveScope().addBinding(
                            ctx.exemplar.getText(), ctx,
                            //give the exemplar symbol a value for itself.
                            exemplarMathType);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.getStart(), ctx.getText());
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
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name, ctx.name.getText());
        }
        return null;
    }

    @Override
    public Void visitTypeRepresentationDecl(ResolveParser.TypeRepresentationDeclContext ctx) {
        symtab.startScope(ctx);
        ParseTree reprTypeNode = ctx.type();
        this.visit(reprTypeNode);

        try {
            curTypeReprModelSymbol =
                    symtab.getInnermostActiveScope().queryForOne(new NameQuery(null, ctx.name.getText(), false))
                            .toTypeModelSymbol();
        } catch (NoSuchSymbolException | UnexpectedSymbolException nsse) {
            //this is actually ok for now. Facility module bound type reprs
            //won't have a model.
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name, ctx.name.getText());
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
            symtab.getInnermostActiveScope()
                    .define(new ProgVariableSymbol(exemplarName, ctx, reprType, getRootModuleIdentifier()));

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
                    curTypeReprModelSymbol, reprType, convention, correspondence);
            reprType.setReprTypeSymbol(rep);
            symtab.getInnermostActiveScope().define(rep);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name, ctx.name.getText());
        }
        curTypeReprModelSymbol = null;
        return null;
    }

    @Override
    public Void visitRecordType(ResolveParser.RecordTypeContext ctx) {
        Map<String, ProgType> fields = new LinkedHashMap<>();
        List<MathClssftnWrappingSymbol> mathSyms = new ArrayList<>();
        //TODO: Maybe instead of fields just use the ProgVariableSymbols...
        for (ResolveParser.RecordVarDeclGroupContext fieldGrp : ctx.recordVarDeclGroup()) {
            this.visit(fieldGrp);
            ProgType grpType = tr.progTypes.get(fieldGrp.type());
            for (TerminalNode t : fieldGrp.ID()) {
                fields.put(t.getText(), grpType);
                //mathSyms.add()
            }
        }
        ProgRecordType record = new ProgRecordType(g, fields);

        MathCartesianClssftn mathVer = (MathCartesianClssftn) record.toMath();
        tr.mathClssftns.put(ctx, mathVer);
        tr.progTypes.put(ctx, record);

        return null;
    }

    @Override
    public Void visitGenericTypeParameterDecl(ResolveParser.GenericTypeParameterDeclContext ctx) {
        try {
            //all generic params are module params; its the only way they can
            //be introduced.
            ModuleParameterSymbol moduleParam =
                    new ModuleParameterSymbol(new ProgParameterSymbol(g, ctx.name.getText(),
                            ParameterMode.TYPE,
                            new ProgGenericType(g, ctx.name.getText()),
                            ctx, getRootModuleIdentifier()));
            symtab.getInnermostActiveScope().define(moduleParam);
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.getStart(), ctx.ID().getText());
        }
        return null;
    }

    @Override
    public Void visitVarDeclGroup(ResolveParser.VarDeclGroupContext ctx) {
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
            } catch (DuplicateSymbolException dse) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, t.getSymbol(), t.getText());
            }
        }
    }

    // prog exprs

    //---------------------------------------------------
    // P R O G    E X P    T Y P I N G
    //---------------------------------------------------

    @Override
    public Void visitProgNestedExp(ResolveParser.ProgNestedExpContext ctx) {
        this.visit(ctx.progExp());
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progExp()));
        tr.mathClssftns.put(ctx, tr.mathClssftns.get(ctx.progExp()));
        return null;
    }

    @Override
    public Void visitProgPrimaryExp(ResolveParser.ProgPrimaryExpContext ctx) {
        this.visit(ctx.progPrimary());
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progPrimary()));
        tr.mathClssftns.put(ctx, tr.mathClssftns.get(ctx.progPrimary()));
        return null;
    }

    @Override
    public Void visitProgPrimary(ResolveParser.ProgPrimaryContext ctx) {
        this.visit(ctx.getChild(0));
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.getChild(0)));
        tr.mathClssftns.put(ctx, tr.mathClssftns.get(ctx.getChild(0)));
        return null;
    }

    @Override
    public Void visitProgSymbolExp(ResolveParser.ProgSymbolExpContext ctx) {
        if (prevSelectorAccess != null) {
            typeProgSelectorAccessExp(ctx, prevSelectorAccess, ctx.name.getText());
            return null;
        }
        try {
            //definition, operation, type, parameter, module param, or just some variable.
            Symbol namedSymbol = null;
            try {
                namedSymbol =
                        symtab.getInnermostActiveScope().queryForOne(
                                new NameQuery(ctx.qualifier, ctx.name.getText(), true));
            } catch (NoSuchModuleException e) {
                //ok, maybe we're dealing with the starting leaf of a prog selector exp
                namedSymbol = symtab.getInnermostActiveScope().queryForOne(
                        new NameQuery(null, ctx.qualifier.getText(), true));
            }
            ProgType programType = ProgInvalidType.getInstance(g);
            ParserRuleContext parentFacilityArgListCtx =
                    Utils.getFirstAncestorOfType(ctx, ResolveParser.ModuleArgumentListContext.class);
            if (namedSymbol instanceof ProgVariableSymbol) {
                programType = ((ProgVariableSymbol) namedSymbol).getProgramType();
            }
            else if (namedSymbol instanceof ProgParameterSymbol) {
                programType = ((ProgParameterSymbol) namedSymbol).getDeclaredType();
            }
            else if (namedSymbol instanceof ProgTypeSymbol) {
                programType = ((ProgTypeSymbol) namedSymbol).getProgramType();

                if (parentFacilityArgListCtx != null) {
                    actualGenericTypesPerFacilitySpecArgs.get(
                            (ResolveParser.ModuleArgumentListContext)
                                    parentFacilityArgListCtx).add((ProgTypeSymbol) namedSymbol);
                }
            }
            //special case (don't want to adapt "mathVariableQuery" to coerce
            //OperationSymbols, so in the meantime
            else if (namedSymbol instanceof OperationSymbol) {
                ProgType returnType = ((OperationSymbol) namedSymbol).getReturnType();
                tr.progTypes.put(ctx, returnType);
                tr.mathClssftns.put(ctx, returnType.toMath());
                return null;
            }
            else if (namedSymbol instanceof ModuleParameterSymbol) {
                programType = ((ModuleParameterSymbol) namedSymbol).getProgramType();
                if (parentFacilityArgListCtx != null &&
                        ((ModuleParameterSymbol) namedSymbol).isModuleTypeParameter()) {
                    actualGenericTypesPerFacilitySpecArgs.get(
                            (ResolveParser.ModuleArgumentListContext)
                                    parentFacilityArgListCtx).add(namedSymbol.toProgTypeSymbol());
                }
            }
            tr.progTypes.put(ctx, programType);
            typeMathSymbol(ctx, ctx.qualifier, ctx.name.getStart());
            if (programType instanceof PTRepresentation) {
                if (((PTRepresentation) programType).getBaseType() instanceof ProgRecordType) {
                    if (Utils.getFirstAncestorOfType(ctx, ResolveParser.ProgSelectorExpContext.class) == null) {
                        //we're something like S.Top.. just a progSymbolExp -- so no need to set the global
                        //prevSelectorAccess, since we're indeed not part of a larger selector exp
                        typeProgSelectorAccessExp(ctx, ctx, ctx.name.getText());
                        prevSelectorAccess = null; //just in case.
                    }
                    else {
                        //if we're a leaf in a larger prog selector access expression, we need to set this
                        //so next time we visit a progSymbolExp we'll call the appropriate method.
                        prevSelectorAccess = ctx;
                    }
                }
            }
            return null;
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(),
                    ctx.name.getText());
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "a variable", ctx.name.getText(),
                    use.getTheUnexpectedSymbolDescription());
        } catch (NoSuchModuleException nsme) {
            noSuchModule(nsme);
        }
        tr.progTypes.put(ctx, ProgInvalidType.getInstance(g));
        tr.mathClssftns.put(ctx, g.INVALID);
        return null;
    }

    private void typeProgSelectorAccessExp(@NotNull ParserRuleContext ctx,
                                           @NotNull ParserRuleContext previousAccess,
                                           @NotNull String fieldName) {
        ProgType prevAccessType = tr.progTypes.get(previousAccess);
        ProgType type = ProgInvalidType.getInstance(g);
        if (prevAccessType instanceof PTRepresentation) {
            ProgType baseType = ((PTRepresentation) prevAccessType).getBaseType();
            try {
                ProgRecordType record = (ProgRecordType) baseType;
                type = record.getFieldType(fieldName);
            } catch (ClassCastException | NoSuchElementException cce) {
                //todo: Error shhould be something like, either previousAccess wasn't record, or field not there
                compiler.errMgr.semanticError(
                        ErrorKind.ILLEGAL_MEMBER_ACCESS, previousAccess.getStart(),
                        previousAccess.getText(), fieldName);
            }
        }
        else if (prevAccessType instanceof ProgRecordType) {
            try {
                type = ((ProgRecordType) prevAccessType).getFieldType(fieldName);
            } catch (NoSuchElementException nse) {
                //remember ctx here (should) be the context of the rightmost/current field access
                compiler.errMgr.semanticError(
                        ErrorKind.NO_SUCH_SYMBOL, ctx.getStart(),
                        ctx.getText());
            }
        }
        else {
            //TODO: Maybe change this one to something like: Not a record at all..
            compiler.errMgr.semanticError(
                    ErrorKind.ILLEGAL_MEMBER_ACCESS, previousAccess.getStart(),
                    previousAccess.getText(), fieldName);
        }
        tr.mathClssftns.put(ctx, type.toMath());
        tr.progTypes.put(ctx, type);
    }

    @Override
    public Void visitProgSelectorExp(ResolveParser.ProgSelectorExpContext ctx) {
        this.visit(ctx.lhs);
        prevSelectorAccess = ctx.lhs;
        this.visit(ctx.rhs);
        prevSelectorAccess = null;

        ProgType finalType = tr.progTypes.get(ctx.rhs);
        MathClssftn finalMathType = tr.mathClssftns.get(ctx.rhs);
        //compiler.info("prog expression: " + ctx.getText() + " of type " + finalType);
        tr.progTypes.put(ctx, finalType);
        tr.mathClssftns.put(ctx, finalMathType);
        return null;
    }

    @Override
    public Void visitProgInfixExp(ResolveParser.ProgInfixExpContext ctx) {
        ctx.progExp().forEach(this::visit);
        List<ProgType> argTypes = Utils.apply(ctx.progExp(), tr.progTypes::get);
        StdTemplateProgOps.BuiltInOpAttributes attr = StdTemplateProgOps.convert(ctx.name.getStart(), argTypes);
        typeOperationRefExp(ctx, attr.qualifier, attr.name, ctx.progExp());
        return null;
    }

    @Override
    public Void visitProgParamExp(ResolveParser.ProgParamExpContext ctx) {
        ctx.progExp().forEach(this::visit);
        typeOperationRefExp(ctx, ctx.progSymbolExp(), ctx.progExp());
        return null;
    }

    @Override
    public Void visitProgBooleanLiteralExp(ResolveParser.ProgBooleanLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Bools", "Boolean");
    }

    @Override
    public Void visitProgIntegerLiteralExp(ResolveParser.ProgIntegerLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Ints", "Integer");
    }

    /*@Override public Void visitProgCharacterLiteralExp(
            ResolveParser.ProgCharacterLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Character_Fac", "Character");
    }
    @Override public Void visitProgStringLiteralExp(
            ResolveParser.ProgStringLiteralExpContext ctx) {
        return typeProgLiteralExp(ctx, "Std_Char_Str_Fac", "Char_Str");
    }*/

    private Void typeProgLiteralExp(ParserRuleContext ctx,
                                    String typeQualifier, String typeName) {
        ProgTypeSymbol p = getProgTypeSymbol(ctx, typeQualifier, typeName);
        tr.progTypes.put(ctx, p != null ? p.getProgramType() : ProgInvalidType.getInstance(g));
        tr.mathClssftns.put(ctx, p != null ? p.getModelType() : g.INVALID);
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
                    new NameQuery(qualifierToken, nameToken.getText(), false))
                    .toProgTypeSymbol();
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(), typeName);
        } catch (UnexpectedSymbolException e) {
            e.printStackTrace();
        } catch (NoSuchModuleException nsme) {
            noSuchModule(nsme);
        }
        return result;
    }

    protected void typeOperationRefExp(@NotNull ParserRuleContext ctx,
                                       @NotNull ResolveParser.ProgSymbolExpContext name,
                                       @NotNull ResolveParser.ProgExpContext... args) {
        typeOperationRefExp(ctx, name, Arrays.asList(args));
    }

    protected void typeOperationRefExp(@NotNull ParserRuleContext ctx,
                                       @NotNull ResolveParser.ProgSymbolExpContext name,
                                       @NotNull List<ResolveParser.ProgExpContext> args) {
        typeOperationRefExp(ctx, name.qualifier, name.name.getStart(), args);
    }

    protected void typeOperationRefExp(@NotNull ParserRuleContext ctx,
                                       @Nullable Token qualifier,
                                       @NotNull Token name,
                                       @NotNull List<ResolveParser.ProgExpContext> args) {
        List<ProgType> argTypes = Utils.apply(args, tr.progTypes::get);
        //every other call
        try {
            OperationSymbol opSym = symtab.getInnermostActiveScope().queryForOne(
                    new OperationQuery(qualifier, name, argTypes,
                            FacilityStrategy.FACILITY_INSTANTIATE,
                            MathSymbolTable.ImportStrategy.IMPORT_NAMED, true));

            tr.progTypes.put(ctx, opSym.getReturnType());
            tr.mathClssftns.put(ctx, opSym.getReturnType().toMath());
            return;
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            List<String> argStrList = Utils.apply(args, ResolveParser.ProgExpContext::getText);
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_OPERATION, name, name.getText(), argStrList, argTypes);
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "an operation", name.getText(),
                    use.getTheUnexpectedSymbolDescription());
        } catch (NoSuchModuleException nsme) {
            noSuchModule(nsme);
        }
        tr.progTypes.put(ctx, ProgInvalidType.getInstance(g));
        tr.mathClssftns.put(ctx, g.INVALID);
    }

    // math constructs

    @Override
    public Void visitMathClssftnTheoremDecl(ResolveParser.MathClssftnTheoremDeclContext ctx) {
        ctx.mathExp().forEach(this::visit);
        MathClssftn x = exactNamedMathClssftns.get(ctx.mathExp(0));
        MathClssftn y = exactNamedMathClssftns.get(ctx.mathExp(1));
        g.relationships.put(x, y);
        return null;
    }

    @Override
    public Void visitMathTheoremDecl(
            ResolveParser.MathTheoremDeclContext ctx) {
        symtab.startScope(ctx);
        this.visit(ctx.mathAssertionExp());
        symtab.endScope();
        MathClssftn x = tr.mathClssftns.get(ctx.mathAssertionExp());
        expectType(ctx.mathAssertionExp(), g.BOOLEAN);
        try {
            //PExp assertion = getMathExpASTFor(ctx.mathAssertionExp());
            symtab.getInnermostActiveScope().define(
                    new TheoremSymbol(g, ctx.name.getText(), g.getTrueExp(),
                            ctx, getRootModuleIdentifier()));
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.name, ctx.name.getText());
        }
        return null;
    }

    @Override
    public Void visitMathCategoricalDefnDecl(ResolveParser.MathCategoricalDefnDeclContext ctx) {
        for (ResolveParser.MathPrefixDefnSigContext sig : ctx.mathPrefixDefnSigs().mathPrefixDefnSig()) {
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

    @Override
    public Void visitMathInductiveDefnDecl(ResolveParser.MathInductiveDefnDeclContext ctx) {
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

    @Override
    public Void visitMathStandardDefnDecl(ResolveParser.MathStandardDefnDeclContext ctx) {
        defnEnclosingScope = symtab.getInnermostActiveScope();
        symtab.startScope(ctx);
        this.visit(ctx.mathDefnSig());
        if (ctx.body != null) this.visit(ctx.body);
        symtab.endScope();
        defnEnclosingScope = null;
        return null;
    }

    @Override
    public Void visitMathDefnSig(ResolveParser.MathDefnSigContext ctx) {
        this.visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitMathInfixDefnSig(ResolveParser.MathInfixDefnSigContext ctx) {
        try {
            insertMathDefnSignature(ctx, ctx.mathVarDecl(), ctx.mathClssftnExp(), ctx.name.getStart());
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.getStart(), e.getOffendingSymbol().getName());
        }
        return null;
    }

    @Override
    public Void visitMathPrefixDefnSig(ResolveParser.MathPrefixDefnSigContext ctx) {
        try {
            insertMathDefnSignature(ctx, ctx.mathVarDeclGroup(),
                    ctx.mathClssftnExp(),
                    Utils.apply(ctx.mathSymbolName(), ParserRuleContext::getStart));
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.getStart(), e.getOffendingSymbol().getName());
        }
        return null;
    }
/*
    @Override
    public Void visitMathPostfixDefnSig(ResolveParser.MathPostfixDefnSigContext ctx) {
        try {
            CommonToken name = new CommonToken(ctx.lop);
            name.setText(ctx.lop.getText() + ".." + ctx.rop.getText());
            insertMathDefnSignature(ctx, ctx.mathVarDecl(),
                    ctx.mathClssftnExp(), name);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), e.getOffendingSymbol().getName());
        }
        return null;
    }*/

    private void insertMathDefnSignature(@NotNull ParserRuleContext ctx,
                                         @NotNull List<? extends ParseTree> formals,
                                         @NotNull ResolveParser.MathClssftnExpContext type,
                                         @NotNull Token... names)
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
        MathClssftn colonRhsType = exactNamedMathClssftns.get(type);

        MathClssftn defnType = null;
        if (colonRhsType.typeRefDepth > 0) {
            int newTypeDepth = colonRhsType.typeRefDepth - 1;
            List<MathClssftn> paramTypes = new ArrayList<>();
            List<String> paramNames = new ArrayList<>();

            if (!formals.isEmpty()) {
                for (ParseTree formal : formals) {
                    try {
                        ResolveParser.MathVarDeclGroupContext grp =
                                (ResolveParser.MathVarDeclGroupContext) formal;
                        for (ResolveParser.MathSymbolNameContext t : grp.mathSymbolName()) {
                            MathClssftn ty = exactNamedMathClssftns.get(grp.mathClssftnExp());
                            paramTypes.add(ty);
                            paramNames.add(t.getText());
                        }
                    } catch (ClassCastException cce) {
                        ResolveParser.MathVarDeclContext singularDecl =
                                (ResolveParser.MathVarDeclContext) formal;
                        MathClssftn ty = exactNamedMathClssftns.get(
                                singularDecl.mathClssftnExp());
                        paramTypes.add(ty);
                        paramNames.add(singularDecl.mathSymbolName().getText());
                    }
                }
                defnType = new MathFunctionClssftn(g, colonRhsType, paramNames, paramTypes);

                for (Token t : names) {
                    MathClssftn asNamed = new MathNamedClssftn(g, t.getText(), newTypeDepth, defnType);
                    defnEnclosingScope.define(new MathClssftnWrappingSymbol(g, t.getText(), asNamed));
                }
            }
            else {
                for (Token t : names) {
                    defnType = new MathNamedClssftn(g, t.getText(), newTypeDepth, colonRhsType);
                    defnEnclosingScope.define(new MathClssftnWrappingSymbol(g, t.getText(), defnType));
                }
            }
        }
        else {
            for (Token t : names) {
                defnEnclosingScope.define(new MathClssftnWrappingSymbol(g, t.getText(), g.INVALID));
            }
        }
    }

    @Override
    public Void visitMathVarDeclGroup(ResolveParser.MathVarDeclGroupContext ctx) {
        insertMathVarDecls(ctx, ctx.mathClssftnExp(), ctx.mathSymbolName());
        return null;
    }

    @Override
    public Void visitMathVarDecl(ResolveParser.MathVarDeclContext ctx) {
        insertMathVarDecls(ctx, ctx.mathClssftnExp(), ctx.mathSymbolName());
        return null;
    }

    private void insertMathVarDecls(@NotNull ParserRuleContext ctx,
                                    @NotNull ResolveParser.MathClssftnExpContext t,
                                    @NotNull ResolveParser.MathSymbolNameContext ... terms) {
        insertMathVarDecls(ctx, t, Arrays.asList(terms));
    }

    private void insertMathVarDecls(@NotNull ParserRuleContext ctx,
                                    @NotNull ResolveParser.MathClssftnExpContext t,
                                    @NotNull List<ResolveParser.MathSymbolNameContext> terms) {
        String x = ctx.getText();
        this.visitMathClssftnExp(t);
        MathClssftn rhsColonType = exactNamedMathClssftns.get(t);
        for (ResolveParser.MathSymbolNameContext term : terms) {
            MathClssftn ty = new MathNamedClssftn(g, term.getText(),
                    rhsColonType.typeRefDepth - 1, rhsColonType);

            ty.identifiesSchematicType = walkingDefnParams &&
                    (rhsColonType == g.SSET || rhsColonType == g.CLS ||
                            rhsColonType instanceof MathPowersetApplicationClssftn);
            try {
                symtab.getInnermostActiveScope().define(new MathClssftnWrappingSymbol(g, term.getText(), ty));
            } catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.getStart(), e.getOffendingSymbol().getName());
            }
        }
    }

    @Override
    public Void visitRequiresClause(ResolveParser.RequiresClauseContext ctx) {
        this.visit(ctx.mathAssertionExp());
        if (ctx.entailsClause() != null) this.visit(ctx.entailsClause());
        return null;
    }

    @Override
    public Void visitMathClssftnExp(ResolveParser.MathClssftnExpContext ctx) {
        walkingType = true;
        this.visit(ctx.mathExp());
        walkingType = false;

        MathClssftn type = exactNamedMathClssftns.get(ctx.mathExp());
        if (type == g.INVALID || type == null || type.getTypeRefDepth() == 0) {
            compiler.errMgr.semanticError(ErrorKind.INVALID_MATH_TYPE, ctx.getStart(), ctx.mathExp().getText());
            type = g.INVALID;
        }
        exactNamedMathClssftns.put(ctx, type);
        tr.mathClssftns.put(ctx, type.enclosingClassification);
        return null;
    }

    private MathClssftn entailsRetype = null;

    @Override
    public Void visitMathClssftnAssertionExp(ResolveParser.MathClssftnAssertionExpContext ctx) {
        this.visit(ctx.mathExp(1)); //visit the asserted clssfctn
        MathClssftn rhsColonType = exactNamedMathClssftns.get(ctx.mathExp(1));
        boolean walkingEntails = Utils.getFirstAncestorOfType(ctx, ResolveParser.EntailsClauseContext.class) != null;
        if (walkingEntails) {
            entailsRetype = rhsColonType;
            this.visit(ctx.mathExp(0));
            entailsRetype = null;
            tr.mathClssftns.put(ctx, g.BOOLEAN);
            //type this guy bool so we can say which_entails x : N AND y : Z
        }
        else if (ctx.mathExp(0).getChild(0).getChild(0) instanceof ResolveParser.MathSymbolExpContext) {
            MathClssftn ty =
                    new MathNamedClssftn(g,
                            ctx.mathExp().get(0).getText(),
                            rhsColonType.typeRefDepth - 1, rhsColonType);
            //TODO TODO
            tr.mathClssftns.put(ctx.mathExp(0), ty.enclosingClassification);
            tr.mathClssftns.put(ctx.mathExp(0).getChild(0), ty.enclosingClassification);
            tr.mathClssftns.put(ctx.mathExp(0).getChild(0).getChild(0), ty.enclosingClassification);

            ty.identifiesSchematicType = true;
            try {
                symtab.getInnermostActiveScope().define(
                        new MathClssftnWrappingSymbol(g, ctx.mathExp().get(0).getText(), ty));
            } catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.getStart(), e.getOffendingSymbol().getName());
            }

            //defnSchematicTypes.put(ctx.ID().getText(), ty);
            exactNamedMathClssftns.put(ctx, ty);
            tr.mathClssftns.put(ctx, ty);
        }
        else {
            compiler.errMgr.semanticError(
                    ErrorKind.ILLEGAL_IMPLICIT_CLSSFTN_PARAM,
                    ctx.getStart(), ctx.getText());
            exactNamedMathClssftns.put(ctx, g.INVALID);
            tr.mathClssftns.put(ctx, g.INVALID);
        }
        return null;
    }

    @Override
    public Void visitMathQuantifiedExp(ResolveParser.MathQuantifiedExpContext ctx) {
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
        exactNamedMathClssftns.put(ctx, exactNamedMathClssftns.get(ctx.mathAssertionExp()));
        return null;
    }

    @Override
    public Void visitMathAssertionExp(ResolveParser.MathAssertionExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.getChild(0));
        return null;
    }

    @Override
    public Void visitMathPrimaryExp(ResolveParser.MathPrimaryExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.mathPrimeExp());
        return null;
    }

    @Override
    public Void visitMathPrimeExp(ResolveParser.MathPrimeExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.getChild(0));
        return null;
    }

    @Override
    public Void visitMathNestedExp(ResolveParser.MathNestedExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.mathAssertionExp());
        return null;
    }

    @Override
    public Void visitMathInfixAppExp(ResolveParser.MathInfixAppExpContext ctx) {
        typeMathFunctionAppExp(ctx, (ParserRuleContext) ctx.getChild(1), ctx.mathExp());
        return null;
    }

    @Override
    public Void visitMathPrefixAppExp(ResolveParser.MathPrefixAppExpContext ctx) {
        typeMathFunctionAppExp(ctx, ctx.name, ctx.mathExp().subList(1, ctx.mathExp().size()));
        return null;
    }

    /*@Override
    public Void visitMathBracketAppExp(ResolveParser.MathBracketAppExpContext ctx) {
        typeMathFunctionAppExp(ctx, ctx.mathSqBrOpExp(), ctx.mathExp());
        return null;
    }*/

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
        MathClssftn t = exactNamedMathClssftns.get(nameExp);
        //if we're a name identifying a function, get our function type.
        if (t instanceof MathNamedClssftn &&
                t.getEnclosingClassification() instanceof MathFunctionClssftn) {
            t = ((MathNamedClssftn) t).enclosingClassification;
        }
        if (!(t instanceof MathFunctionClssftn)) {
            if (t != g.INVALID && t.enclosingClassification != g.INVALID) { //only tell users if its a meaning 'non-function' classification
                compiler.errMgr.semanticError(ErrorKind.APPLYING_NON_FUNCTION,
                        nameExp.getStart(), nameExp.getText());
            }
            exactNamedMathClssftns.put(ctx, g.INVALID);
            tr.mathClssftns.put(ctx, g.INVALID);
            return;
        }
        MathFunctionClssftn expectedFuncType = (MathFunctionClssftn) t;
        List<MathClssftn> actualArgumentTypes = Utils.apply(args, tr.mathClssftns::get);
        List<MathClssftn> formalParameterTypes = expectedFuncType.getParamTypes();
        //ugly hook to handle chained operator applications like 0 <= i <= j or
        //j >= i >= 0
        if ((nameExp.getText().equals("<=") || nameExp.getText().equals("<") || nameExp.getText().equals("≤")) &&
                args.size() == 2 &&
                args.get(0) instanceof ResolveParser.MathInfixAppExpContext) {
            ResolveParser.MathInfixAppExpContext argAsInfixApp =
                    (ResolveParser.MathInfixAppExpContext) args.get(0);
            if (argAsInfixApp.getChild(1).getText().equals("<=") ||
                    argAsInfixApp.getChild(1).getText().equals("≤") ||
                    argAsInfixApp.getChild(1).getText().equals("<")) {
                MathClssftn x = tr.mathClssftns.get(argAsInfixApp.getChild(2));
                actualArgumentTypes.set(0, x);
            }
        } //end ugly hook.
        //TODO: Factor this out to a helper, get it out of my face.
        if (formalParameterTypes.size() != actualArgumentTypes.size()) {
            compiler.errMgr.semanticError(ErrorKind.INCORRECT_FUNCTION_ARG_COUNT, ctx.getStart(), ctx.getText());
            exactNamedMathClssftns.put(ctx, g.INVALID);
            tr.mathClssftns.put(ctx, g.INVALID);
            return;
        }
        try {
            MathClssftn oldExpectedFuncType = expectedFuncType;

            expectedFuncType = (MathFunctionClssftn)
                    expectedFuncType.deschematize(actualArgumentTypes);
            if (!oldExpectedFuncType.toString().equals(expectedFuncType.toString())) {
                compiler.log("expected function type: " + oldExpectedFuncType);
                compiler.log("   deschematizes to: " + expectedFuncType);
            }
        } catch (BindingException e) {
            compiler.log("formal params in: '" + asString +
                    "' don't bind against the actual arg types");
        }
        //we have to redo this since deschematize above might've changed the
        //args
        formalParameterTypes = expectedFuncType.getParamTypes();

        List<MathClssftn> actualValues = Utils.apply(args, exactNamedMathClssftns::get);

        Iterator<? extends ParseTree> actualsCtxIter = args.iterator();
        Iterator<MathClssftn> actualsIter = actualArgumentTypes.iterator();
        Iterator<MathClssftn> formalsIter = formalParameterTypes.iterator();
        Iterator<MathClssftn> actualValuesIter = actualValues.iterator();

        //SUBTYPE AND EQUALITY CHECK FOR ARGS HAPPENS HERE
        while (actualsIter.hasNext()) {
            MathClssftn actual = actualsIter.next();
            MathClssftn formal = formalsIter.next();
            ParserRuleContext argCtx = (ParserRuleContext) actualsCtxIter.next();

            if (!g.isSubtype(actual, formal)) {
                compiler.errMgr.semanticError(
                        ErrorKind.INVALID_APPLICATION_ARG, argCtx.getStart(),
                        actual.toString(), formal.toString());
            }

            MathClssftn actualVal = actualValuesIter.next();
            //if someone tries to pass a literal (say, 'true') for some
            //formal x : SSET ... we need a notion of 'value' to check this.
            //the if below is where this happens.
            if (actualVal != null && actualVal != g.INVALID
                    && actualVal.typeRefDepth == 0
                    && formal.typeRefDepth >= 2) {
                //its ok if we're a schematic type whose enclosing classification is a set
                if (actualVal.identifiesSchematicType && actualVal.enclosingClassification.typeRefDepth >= 1) {
                    continue;
                }
                compiler.errMgr.semanticError(
                        ErrorKind.INVALID_APPLICATION_ARG2, argCtx.getStart(),
                        actualVal.toString());
            }
        }

        //If we're describing a type, then the range (as a result of the function is too broad),
        //so we'll annotate the type of this application with its (verbose) application type.
        //but it's enclosing type will of course still be the range.
        if (walkingType && expectedFuncType.getRangeClssftn().getTypeRefDepth() <= 1) {
            exactNamedMathClssftns.put(ctx, g.INVALID);
            tr.mathClssftns.put(ctx, g.INVALID);
        }
        else if (walkingType) {
            List<MathClssftn> actualNamedArgumentTypes =
                    Utils.apply(args, exactNamedMathClssftns::get);
            MathClssftn appType =
                    expectedFuncType.getApplicationType(
                            nameExp.getText(), actualNamedArgumentTypes);
            exactNamedMathClssftns.put(ctx, appType);
            tr.mathClssftns.put(ctx, appType);
        }
        else {
            //the classification of an f-application exp is the range of f,
            //according to the rule:
            //  C \ f : C x D -> R
            //  C \ E1 : C
            //  C \ E2 : D
            //  ---------------------
            //  C \ f(E1, E2) : R
            exactNamedMathClssftns.put(ctx, expectedFuncType.getRangeClssftn());
            tr.mathClssftns.put(ctx, expectedFuncType.getRangeClssftn());
        }
    }

    @Override
    public Void visitMathSymbolExp(ResolveParser.MathSymbolExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.name.getStart());
        return null;
    }

    @Override
    public Void visitMathCartProdExp(ResolveParser.MathCartProdExpContext ctx) {
        List<Element> fields = new ArrayList<>();
        for (ResolveParser.MathVarDeclGroupContext grp : ctx.mathVarDeclGroup()) {
            this.visit(grp.mathClssftnExp());
            MathClssftn grpType = exactNamedMathClssftns.get(grp.mathClssftnExp());
            for (ResolveParser.MathSymbolNameContext label : grp.mathSymbolName()) {
                fields.add(new Element(label.getText(), grpType));
            }
        }
        MathCartesianClssftn cartClssftn = new MathCartesianClssftn(g, fields);
        tr.mathClssftns.put(ctx, cartClssftn);
        exactNamedMathClssftns.put(ctx, cartClssftn);
        return null;
    }

    @Override
    public Void visitMathSetRestrictionExp(ResolveParser.MathSetRestrictionExpContext ctx) {
        this.visit(ctx.mathVarDecl());
        this.visit(ctx.mathAssertionExp());
        MathClssftn t =
                g.POWERSET_FUNCTION.getApplicationType("Powerset",
                        exactNamedMathClssftns.get(
                                ctx.mathVarDecl().mathClssftnExp()));
        exactNamedMathClssftns.put(ctx, t);
        tr.mathClssftns.put(ctx, t);
        return null;
    }

    @Override
    public Void visitMathSetExp(ResolveParser.MathSetExpContext ctx) {
        ctx.mathExp().forEach(this::visit);
        if (ctx.mathExp().isEmpty()) {
            tr.mathClssftns.put(ctx, g.EMPTY_SET);
        }
        else {
            MathClssftn t = g.POWERSET_FUNCTION.getApplicationType(
                    "Powerset", exactNamedMathClssftns.get(ctx.mathExp(0)));
            tr.mathClssftns.put(ctx, t);
        }
        return null;
    }

    @Override
    public Void visitMathLambdaExp(ResolveParser.MathLambdaExpContext ctx) {
        symtab.startScope(ctx);
        compiler.log("lambda exp: " + ctx.getText());

        walkingDefnParams = true;
        //activeQuantifications.push(Quantification.UNIVERSAL);
        this.visit(ctx.mathVarDecl());
        //activeQuantifications.pop();
        walkingDefnParams = false;

        this.visit(ctx.mathExp());
        symtab.endScope();

        MathClssftn bodyClss = tr.mathClssftns.get(ctx.mathExp());
        MathClssftn varClss = exactNamedMathClssftns.get(ctx.mathVarDecl().mathClssftnExp());
        tr.mathClssftns.put(ctx, new MathFunctionClssftn(g, bodyClss, varClss));
        return null;
    }

    @Override
    public Void visitMathAlternativeExp(ResolveParser.MathAlternativeExpContext ctx) {

        MathClssftn establishedType = null;
        MathClssftn establishedTypeValue = null;
        for (ResolveParser.MathAlternativeItemExpContext alt : ctx.mathAlternativeItemExp()) {
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

    @Override
    public Void visitMathAlternativeItemExp(ResolveParser.MathAlternativeItemExpContext ctx) {
        if (ctx.condition != null) {
            expectType(ctx.condition, g.BOOLEAN);
        }
        tr.mathClssftns.put(ctx, tr.mathClssftns.get(ctx.result));
        return null;
    }

    @Override
    public Void visitMathSelectorExp(ResolveParser.MathSelectorExpContext ctx) {
        MathClssftn tempEntailsRetype = entailsRetype;
        entailsRetype = null;
        this.visit(ctx.lhs);
        prevSelectorAccess = ctx.lhs;

        entailsRetype = tempEntailsRetype;
        this.visit(ctx.rhs);
        prevSelectorAccess = null;

        MathClssftn finalClassfctn = tr.mathClssftns.get(ctx.rhs);
        tr.mathClssftns.put(ctx, finalClassfctn);
        exactNamedMathClssftns.put(ctx, finalClassfctn);
        return null;
    }

    private void typeMathSelectorAccessExp(@NotNull ParserRuleContext ctx,
                                           @NotNull ParserRuleContext prevAccessExp,
                                           @NotNull String symbolName) {

        MathClssftn type;
        MathClssftn prevMathAccessType = tr.mathClssftns.get(prevAccessExp);
        //Todo: This can't go into {@link #getMetaFieldType()} since
        //it starts the access chain, rather than, say, terminating it.
        if (prevAccessExp.getText().equals("conc")) {
            if (curTypeReprModelSymbol == null) {
                compiler.errMgr.semanticError(ErrorKind.NO_SUCH_FACTOR, ctx.getStart(), symbolName);
                tr.mathClssftns.put(ctx, g.INVALID);
                return;
            }
            tr.mathClssftns.put(ctx, curTypeReprModelSymbol.getModelType());
            return;
        }
        try {
            MathCartesianClssftn typeCartesian = (MathCartesianClssftn) prevMathAccessType;
            if (entailsRetype != null) {
                Element x = typeCartesian.getElementUnder(symbolName);
                if (x != null) {
                    x.clssfcn = entailsRetype;
                }
                //if (x != null) typeCartesian.tagsToElements.put(symbolName, x);
            }
            type = typeCartesian.getFactor(symbolName);
        } catch (ClassCastException | NoSuchElementException cce) {
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

    private void typeMathSymbol(@NotNull ParserRuleContext ctx, @Nullable Token qualifier, @NotNull Token name) {
        String here = ctx.getText();
        //Add initial check if here to see if the 'qualifier' refers to a record/cartesian.
        if (qualifier != null && prevSelectorAccess == null) {
            try {
                MathClssftnWrappingSymbol s = symtab.getInnermostActiveScope()
                        .queryForOne(new MathSymbolQuery(null, qualifier.getText(), ctx.getStart()));
                if (s.getClassification().enclosingClassification instanceof MathCartesianClssftn
                        || qualifier.getText().equals("conc")) {
                    tr.mathClssftns.put(ctx, s.getClassification().enclosingClassification);
                    typeMathSelectorAccessExp(ctx, ctx, name.getText());
                    //If we're part of a larger selector exp, remember to set the global var
                    if (Utils.getFirstAncestorOfType(ctx, ResolveParser.MathSelectorExpContext.class) == null) {
                        prevSelectorAccess = null;
                    }
                    else {
                        prevSelectorAccess = ctx;
                    }
                    return; //we've typed the leaf selector and set the global selector access var appropriately,
                    //nothing left to do here at this point.
                }
            }
            catch (Exception e) {   //no problem, we'll proceed under the assumption that in the case of something
                //like S.X, the S actually identifies a module (couldn't find anything else above).
            }
        }
        if (prevSelectorAccess != null) {
            typeMathSelectorAccessExp(ctx, prevSelectorAccess, name.getText());
        }
        else {
            MathClssftnWrappingSymbol s = getIntendedMathSymbol(qualifier, name, ctx);
            if (s == null || s.getClassification() == null) {
                exactNamedMathClssftns.put(ctx, g.INVALID);
                tr.mathClssftns.put(ctx, g.INVALID);
                return;
            }
            if (entailsRetype != null) {
                s.setClassification(new MathNamedClssftn(g, name.getText(), entailsRetype.typeRefDepth - 1, entailsRetype));
            }
            exactNamedMathClssftns.put(ctx, s.getClassification());
            if (s.getClassification().identifiesSchematicType) {
                tr.mathClssftns.put(ctx, s.getClassification());
            }
            else {
                tr.mathClssftns.put(ctx, s.getClassification().getEnclosingClassification());
            }
        }
    }

    @Nullable
    private MathClssftnWrappingSymbol getIntendedMathSymbol(@Nullable Token qualifier,
                                                            @NotNull Token symbolName,
                                                            @NotNull ParserRuleContext ctx) {
        try {
            return symtab.getInnermostActiveScope()
                    .queryForOne(new MathSymbolQuery(qualifier, symbolName.getText(), ctx.getStart()));
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), symbolName, symbolName.getText());
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

    public static MathClssftn getMetaFieldType(
            @NotNull DumbMathClssftnHandler g, @NotNull String metaSegment) {
        MathClssftn result = null;

        if (metaSegment.equals("Is_Initial")) {
            result = new MathFunctionClssftn(g, g.BOOLEAN, g.ENTITY);
        }
        else if (metaSegment.equals("base_point")) {
            result = g.ENTITY;
        }
        return result;
    }

    private void expectType(ParserRuleContext ctx, MathClssftn expected) {
        MathClssftn foundType = tr.mathClssftns.get(ctx);
        if (!g.isSubtype(foundType, expected)) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_TYPE, ctx.getStart(), expected, foundType);
        }
    }

    /**
     * Given some context {@code ctx} and a
     * {@code child} context; this method visits {@code child} and chains/passes
     * its found {@link MathClssftn} upto {@code ctx}.
     *
     * @param ctx   a parent {@code ParseTree}
     * @param child one of {@code ctx}s children
     */
    private void visitAndClassifyMathExpCtx(@NotNull ParseTree ctx, @NotNull ParseTree child) {
        this.visit(child);
        exactNamedMathClssftns.put(ctx, exactNamedMathClssftns.get(child));
        MathClssftn x = tr.mathClssftns.get(child);
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

    private void sanityCheckStmtsForReturn(@NotNull Token operationName,
                                           @Nullable ParserRuleContext type,
                                           @NotNull List<ResolveParser.StmtContext> stmts) {
        boolean missingReturn = false;
        if (type == null) return;
        if (stmts.size() == 0) missingReturn = true;
        if (stmts.size() > 0) {
            ResolveParser.StmtContext lastStmt = stmts.get(stmts.size() - 1);
            if (lastStmt.getChild(0) instanceof ResolveParser.AssignStmtContext) {
                ResolveParser.AssignStmtContext lastAsAssign =
                        (ResolveParser.AssignStmtContext) lastStmt.getChild(0);
                if (!lastAsAssign.left.getText().equals(operationName.getText())) {
                    missingReturn = true;
                }
            }
        }
        if (missingReturn) {
            compiler.errMgr.semanticError(ErrorKind.MISSING_RETURN_STMT, operationName, operationName.getText());
        }
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
