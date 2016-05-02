package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.*;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.misc.HardCodedProgOps;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.semantics.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.programtype.ProgNamedType;
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.semantics.query.NameQuery;
import edu.clemson.resolve.semantics.query.SymbolTypeQuery;
import edu.clemson.resolve.semantics.query.UnqualifiedNameQuery;
import edu.clemson.resolve.semantics.symbol.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.clemson.resolve.codegen.model.AccessRef.LeafAccessRefLeft;
import static edu.clemson.resolve.codegen.model.AccessRef.LeafAccessRefRight;
import static edu.clemson.resolve.codegen.model.Qualifier.FacilityQualifier;
import static edu.clemson.resolve.codegen.model.Qualifier.NormalQualifier;
import static edu.clemson.resolve.codegen.model.Stat.*;

public class ModelBuilder extends ResolveBaseListener {

    public ParseTreeProperty<OutputModelObject> built =
            new ParseTreeProperty<>();

    private final JavaCodeGenerator gen;
    private final MathSymbolTable symtab;

    @NotNull
    private final AnnotatedModule tr;
    @NotNull
    private final RESOLVECompiler compiler;

    private ModuleScopeBuilder moduleScope;

    public ModelBuilder(@NotNull JavaCodeGenerator gen,
                        @NotNull MathSymbolTable symtab) {
        this.gen = gen;
        this.symtab = symtab;
        this.compiler = gen.compiler;
        this.tr = gen.getModule();
        try {
            this.moduleScope = symtab.getModuleScope(
                    new ModuleIdentifier(tr.getNameToken()));
        } catch (NoSuchModuleException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exitModuleDecl(ResolveParser.ModuleDeclContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override
    public void exitTypeModelDecl(
            ResolveParser.TypeModelDeclContext ctx) {
        built.put(ctx, new TypeInterfaceDef(ctx.name.getText()));
    }

    @Override
    public void exitOperationDecl(
            ResolveParser.OperationDeclContext ctx) {
        FunctionDef f = new FunctionDef(ctx.name.getText());
        f.hasReturn = ctx.type() != null;
        f.isStatic = withinFacilityModule();
        for (ResolveParser.ParameterDeclGroupContext grp : ctx
                .operationParameterList().parameterDeclGroup()) {
            for (TerminalNode id : grp.ID()) {
                f.params.add((ParameterDef) built.get(id));
            }
        }
        built.put(ctx, f);
    }

    @Override
    public void exitOperationProcedureDecl(
            ResolveParser.OperationProcedureDeclContext ctx) {
        FunctionImpl f =
                buildFunctionImpl(ctx.name.getText(),
                        ctx.type(), ctx.operationParameterList()
                                .parameterDeclGroup(), ctx.varDeclGroup(),
                        ctx.stmt());
        built.put(ctx, f);
    }

    @Override
    public void exitProcedureDecl(
            ResolveParser.ProcedureDeclContext ctx) {
        FunctionImpl f =
                buildFunctionImpl(ctx.name.getText(),
                        ctx.type(), ctx.operationParameterList()
                                .parameterDeclGroup(), ctx.varDeclGroup(),
                        ctx.stmt());
        f.implementsOper = true;
        built.put(ctx, f);
    }

    protected FunctionImpl buildFunctionImpl(@NotNull String name,
                                             @Nullable ResolveParser.TypeContext type,
                                             @NotNull List<ResolveParser.ParameterDeclGroupContext> paramGroupings,
                                             @NotNull List<ResolveParser.VarDeclGroupContext> variableGroupings,
                                             @NotNull List<ResolveParser.StmtContext> stats) {
        FunctionImpl f = new FunctionImpl(name);
        f.hasReturn = type != null;
        f.isStatic = withinFacilityModule();
        for (ResolveParser.ParameterDeclGroupContext grp : paramGroupings) {
            f.params.addAll(Utils.collect(ParameterDef.class, grp.ID(), built));
        }
        for (ResolveParser.VarDeclGroupContext grp : variableGroupings) {
            f.vars.addAll(Utils.collect(VariableDef.class, grp.ID(), built));
        }
        for (ResolveParser.StmtContext s : stats) {
            f.stats.add((Stat) built.get(s));
            //ResolveParser returns are buried in an assignment
            //(specifically those assignments whose lhs == funcname)
            if (s.assignStmt() != null
                    && s.assignStmt().left.getText().equals(name)
                    && f.hasReturn) {
                Expr rhs = (Expr) built.get(s.assignStmt().right);
                f.vars.add(new VariableDef(name, rhs));
                f.stats.add(new ReturnStat(name));
            }
        }
        return f;
    }

    @Override
    public void exitFacilityDecl(
            ResolveParser.FacilityDeclContext ctx) {
        FacilityDef f = new FacilityDef(ctx.name.getText(), ctx.spec.getText());
        f.isStatic = withinFacilityModule();
        List<DecoratedFacilityInstantiation> layers = new ArrayList<>();

        DecoratedFacilityInstantiation basePtr =
                new DecoratedFacilityInstantiation(ctx.spec.getText(),
                        ctx.impl.getText());
        basePtr.isProxied = false;
        List<Expr> specArgs =
                ctx.specArgs == null ? new ArrayList<>() : Utils.collect(
                        Expr.class, ctx.specArgs.progExp(), built);
        List<Expr> implArgs =
                ctx.implArgs == null ? new ArrayList<>() : Utils.collect(
                        Expr.class, ctx.implArgs.progExp(), built);
        basePtr.args.addAll(specArgs);
        basePtr.args.addAll(implArgs);

        for (ResolveParser.ExtensionPairingContext pair :
                ctx.extensionPairing()) {
            DecoratedFacilityInstantiation layer =
                    new DecoratedFacilityInstantiation(pair.spec.getText(),
                            pair.impl.getText());
            specArgs = pair.specArgs == null ? new ArrayList<>() :
                    Utils.collect(Expr.class, pair.specArgs.progExp(), built);
            implArgs = pair.implArgs == null ? new ArrayList<>() :
                    Utils.collect(Expr.class, pair.implArgs.progExp(), built);
            layer.args.addAll(basePtr.args); // always prefaced with the base facility args
            layer.args.addAll(specArgs);
            layer.args.addAll(implArgs);
            layers.add(layer);
        }

        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).isProxied = ctx.extensionPairing().size() > 1;
            if (i + 1 < layers.size()) {
                layers.get(i).child = layers.get(i + 1);
            }
            else {
                layers.get(i).child = basePtr;
            }
        }
        f.root = layers.isEmpty() ? basePtr : layers.get(0);
        built.put(ctx, f);
    }

    @Override
    public void exitVarDeclGroup(
            ResolveParser.VarDeclGroupContext ctx) {
        Expr init = (Expr) built.get(ctx.type());
        for (TerminalNode t : ctx.ID()) {
            //System.out.println("adding "+t.getText()+" to built map");
            built.put(t, new VariableDef(t.getSymbol().getText(), init));
        }
    }

    @Override
    public void exitRecordVarDeclGroup(
            ResolveParser.RecordVarDeclGroupContext ctx) {
        Expr init = (Expr) built.get(ctx.type());
        for (TerminalNode t : ctx.ID()) {
            //System.out.println("adding "+t.getText()+" to built map");
            built.put(t, new VariableDef(t.getSymbol().getText(), init));
        }
    }

    @Override
    public void exitNamedType(ResolveParser.NamedTypeContext ctx) {
        built.put(ctx, new TypeInit(buildQualifier(
                ctx.qualifier, ctx.name), ctx.name.getText(), ""));
    }

    @Override
    public void exitTypeRepresentationDecl(
            ResolveParser.TypeRepresentationDeclContext ctx) {
        MemberClassDef representationClass =
                new MemberClassDef(ctx.name.getText());
        String exemplarName = "";
        try {
            ProgReprTypeSymbol x =
                    moduleScope.queryForOne(
                            new UnqualifiedNameQuery(ctx.name.getText()))
                            .toProgReprTypeSymbol();
            exemplarName =
                    ((ProgNamedType) x.toProgTypeSymbol().getProgramType())
                            .getExemplarName();
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            exemplarName = ctx.name.getText().substring(0, 1); //default name
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.name, "a type representation", ctx.name.getText(),
                    use.getTheUnexpectedSymbolDescription());
        } catch (NoSuchModuleException e) {
            e.printStackTrace();
        }
        representationClass.isStatic = withinFacilityModule();
        representationClass.referredToByExemplar = exemplarName;
        if (ctx.type() instanceof ResolveParser.RecordTypeContext) {
            ResolveParser.RecordTypeContext typeAsRecord =
                    (ResolveParser.RecordTypeContext) ctx.type();
            for (ResolveParser.RecordVarDeclGroupContext grp :
                    typeAsRecord.recordVarDeclGroup()) {
                representationClass.fields.addAll(Utils.collect(
                        VariableDef.class, grp.ID(), built));
            }
        }
        if (ctx.typeImplInit() != null) {
            //representationClass.initVars.addAll(Utils.collect(
            //        VariableDef.class, ctx.typeImplInit().variableDeclGroup(),
            //        built));
            for (ResolveParser.VarDeclGroupContext grp :
                    ctx.typeImplInit().varDeclGroup()) {
                for (TerminalNode t : grp.ID()) {
                    representationClass.initVars.add((VariableDef) built.get(t));
                }
            }
            //representationClass.initStats.addAll(Utils.collect(Stat.class, ctx
            //        .typeImplInit().stmt(), built));
        }
        built.put(ctx, representationClass);
    }

    @Override
    public void exitParameterDeclGroup(
            ResolveParser.ParameterDeclGroupContext ctx) {
        for (TerminalNode t : ctx.ID()) {
            built.put(t, new ParameterDef(t.getText()));
        }
    }

    @Override
    public void exitStmt(ResolveParser.StmtContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override
    public void exitAssignStmt(ResolveParser.AssignStmtContext ctx) {
        built.put(ctx, buildPrimitiveInfixStat("assign", ctx.left, ctx.right));
    }

    @Override
    public void exitSwapStmt(ResolveParser.SwapStmtContext ctx) {
        built.put(ctx, buildPrimitiveInfixStat("swap", ctx.left, ctx.right));
    }

    @Override
    public void exitCallStmt(ResolveParser.CallStmtContext ctx) {
        built.put(ctx, new CallStat((Expr) built.get(ctx.progParamExp())));
    }

    @Override
    public void exitWhileStmt(
            ResolveParser.WhileStmtContext ctx) {
        WhileStat w = new WhileStat((Expr) built.get(ctx.progExp()));
        w.stats.addAll(Utils.collect(Stat.class, ctx.stmt(), built));
        built.put(ctx, w);
    }

    @Override
    public void exitIfStmt(
            ResolveParser.IfStmtContext ctx) {
        IfStat i = new IfStat((Expr) built.get(ctx.progExp()));
        i.ifStats.addAll(Utils.collect(Stat.class, ctx.stmt(), built));
        if (ctx.elseStmt() != null) {
            i.elseStats.addAll(Utils.collect(Stat.class,
                    ctx.elseStmt().stmt(), built));
        }
        built.put(ctx, i);
    }

    @Override
    public void exitProgNestedExp(
            ResolveParser.ProgNestedExpContext ctx) {
        built.put(ctx, built.get(ctx.progExp()));
    }

    @Override
    public void exitProgPrimaryExp(
            ResolveParser.ProgPrimaryExpContext ctx) {
        built.put(ctx, built.get(ctx.progPrimary()));
    }

    @Override
    public void exitProgPrimary(
            ResolveParser.ProgPrimaryContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override
    public void exitProgParamExp(
            ResolveParser.ProgParamExpContext ctx) {
        List<Expr> args = Utils.collect(Expr.class, ctx.progExp(), built);
        if (referencesOperationParameter(ctx.progNamedExp().name.getText())) {
            built.put(ctx, new MethodCall.OperationParameterMethodCall(
                    ctx.progNamedExp().name.getText(), args));
        }
        else {
            built.put(ctx, new MethodCall(buildQualifier(
                    ctx.progNamedExp().qualifier, ctx.progNamedExp().name),
                    ctx.progNamedExp().name.getText(), args));
        }
    }

    private boolean referencesOperationParameter(String name) {
        List<ModuleParameterSymbol> moduleParams =
                moduleScope.getSymbolsOfType(ModuleParameterSymbol.class);
        for (ModuleParameterSymbol p : moduleParams) {
            if (p.isModuleOperationParameter() && p.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void exitProgInfixExp(
            ResolveParser.ProgInfixExpContext ctx) {
        built.put(ctx, buildSugaredProgExp(ctx, ctx.op, ctx.progExp()));
    }

    private MethodCall buildSugaredProgExp(@NotNull ParserRuleContext ctx,
                                           @NotNull Token op,
                                           @NotNull ParseTree... args) {
        return buildSugaredProgExp(ctx, op, Arrays.asList(args));
    }

    private MethodCall buildSugaredProgExp(@NotNull ParserRuleContext ctx,
                                           @NotNull Token op,
                                           @NotNull List<? extends ParseTree> args) {
        List<ProgType> argTypes = args.stream().map(tr.progTypes::get)
                .collect(Collectors.toList());
        HardCodedProgOps.BuiltInOpAttributes o =
                HardCodedProgOps.convert(op, argTypes);
        return new MethodCall(buildQualifier(o.qualifier, o.name),
                o.name.getText(), Utils.collect(Expr.class, args, built));
    }

    @Override
    public void exitProgNamedExp(
            ResolveParser.ProgNamedExpContext ctx) {
        //if we're within a module argument list:
        if (Utils.getFirstAncestorOfType(ctx,
                ResolveParser.ModuleArgumentListContext.class) != null) {
            built.put(ctx, createFacilityArgumentModel(ctx));
        }
        else {
            built.put(ctx, new VarNameRef(new NormalQualifier("this"),
                    ctx.name.getText()));
        }
    }

    @Override
    public void exitProgSelectorExp(
            ResolveParser.ProgSelectorExpContext ctx) {
        ProgType leftType = tr.progTypes.get(ctx.lhs);
        Expr left = new LeafAccessRefLeft(((ProgNamedType) leftType).getName(),
                (Expr) built.get(ctx.lhs));
        Expr right = new LeafAccessRefRight((Expr) built.get(ctx.rhs));
        AccessRef ref = new AccessRef(left, right);
        built.put(ctx, ref);
    }

    /**
     * Given an arbitrary expression within some
     * {@link ResolveParser.ModuleArgumentListContext}, returns an {@link OutputModelObject}
     * suitable for that argument.
     */
    @NotNull
    private OutputModelObject createFacilityArgumentModel(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {
        OutputModelObject result = null;
        try {
            Symbol s = moduleScope
                    .queryForOne(new NameQuery(ctx.qualifier, ctx.name, true));
            if (s instanceof OperationSymbol || s.isModuleOperationParameter()) {
                result = new AnonOpParameterClassInstance(buildQualifier(
                        ctx.qualifier, ctx.name), s.toOperationSymbol());
            }
            else if (s.isModuleTypeParameter()) {
                //typeinit wrapped in a "get" call
                result = new MethodCall(new TypeInit(buildQualifier(
                        ctx.qualifier, ctx.name), ctx.name.getText(), ""));
            }
            else if (s instanceof ProgTypeSymbol || s instanceof ProgReprTypeSymbol) {
                result = new TypeInit(buildQualifier(
                        ctx.qualifier, ctx.name), ctx.name.getText(), "");
            }
            else {
                result = new VarNameRef(new NormalQualifier("this"),
                        ctx.name.getText());
            }
        } catch (SymbolTableException e) {
            throw new RuntimeException();//shouldn't happen now
        }
        return result;
    }

    @Override
    public void exitProgBooleanLiteralExp(
            ResolveParser.ProgBooleanLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier(
                "Boolean_Template", "Std_Bools"), "Boolean", ctx.getText()));
    }

    @Override
    public void exitProgIntegerLiteralExp(
            ResolveParser.ProgIntegerLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier(
                "Integer_Template", "Std_Ints"), "Integer", ctx.getText()));
    }

    @Override
    public void exitProgCharacterLiteralExp(
            ResolveParser.ProgCharacterLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier(
                "Character_Template", "Std_Chars"), "Character", ctx.getText()));
    }

    @Override
    public void exitProgStringLiteralExp(
            ResolveParser.ProgStringLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier(
                "Char_String_Template", "Std_Char_Strings"), "Char_Str", ctx.getText()));
    }

    @Override
    public void exitConceptImplModuleDecl(
            ResolveParser.ConceptImplModuleDeclContext ctx) {
        ModuleFile file = buildFile();
        ConceptImplModule impl =
                new ConceptImplModule(ctx.name.getText(),
                        ctx.concept.getText(), file);
        if (ctx.implBlock() != null) {
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().procedureDecl(), built));
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().operationProcedureDecl(), built));
            impl.repClasses.addAll(Utils.collect(MemberClassDef.class, ctx
                    .implBlock().typeRepresentationDecl(), built));
            impl.facilityVars.addAll(Utils.collect(FacilityDef.class, ctx
                    .implBlock().facilityDecl(), built));
        }
        List<ModuleParameterSymbol> allParamsFromSpecAndImpl = null;
        try {
            allParamsFromSpecAndImpl =
                    symtab.getModuleScope(new ModuleIdentifier(ctx.concept))
                            .getSymbolsOfType(ModuleParameterSymbol.class);
            allParamsFromSpecAndImpl.addAll(moduleScope
                    .getSymbolsOfType(ModuleParameterSymbol.class));
            impl.addGettersAndMembersForModuleParameterSyms(
                    allParamsFromSpecAndImpl);
        } catch (NoSuchModuleException e) { //shouldn't happen
        }
        impl.addCtor();
        file.module = impl;
        built.put(ctx, file);
    }

    @Override
    public void exitFacilityModuleDecl(
            ResolveParser.FacilityModuleDeclContext ctx) {
        ModuleFile file = buildFile();
        FacilityImplModule impl =
                new FacilityImplModule(ctx.name.getText(), file);

        if (ctx.facilityBlock() != null) {
            impl.facilities.addAll(Utils.collect(FacilityDef.class, ctx
                    .facilityBlock().facilityDecl(), built));
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .facilityBlock().operationProcedureDecl(), built));
            impl.repClasses.addAll(Utils.collect(MemberClassDef.class, ctx
                    .facilityBlock().typeRepresentationDecl(), built));
        }
        file.module = impl;
        built.put(ctx, file);
    }

    @Override
    public void exitConceptModuleDecl(
            ResolveParser.ConceptModuleDeclContext ctx) {
        ModuleFile file = buildFile();
        SpecModule spec = new SpecModule.ConceptModule(ctx.name.getText(), file);

        if (ctx.conceptBlock() != null) {
            spec.types.addAll(Utils.collect(TypeInterfaceDef.class, ctx
                    .conceptBlock().typeModelDecl(), built));
            spec.funcs.addAll(Utils.collect(FunctionDef.class, ctx
                    .conceptBlock().operationDecl(), built));
        }
        try {
            spec.addGettersAndMembersForModuleParameterSyms(moduleScope
                    .query(new SymbolTypeQuery<>(ModuleParameterSymbol.class)));
        } catch (NoSuchModuleException | UnexpectedSymbolException e) {
        }
        file.module = spec;
        built.put(ctx, file);
    }

    @Override
    public void exitConceptExtModuleDecl(
            ResolveParser.ConceptExtModuleDeclContext ctx) {
        ModuleFile file = buildFile();
        SpecModule spec = new SpecModule.ExtensionModule(ctx.name.getText(),
                ctx.concept.getText(), file);

        if (ctx.conceptBlock() != null) {
            spec.types.addAll(Utils.collect(TypeInterfaceDef.class, ctx
                    .conceptBlock().typeModelDecl(), built));
            spec.funcs.addAll(Utils.collect(FunctionDef.class, ctx
                    .conceptBlock().operationDecl(), built));
        }
        //Note that here we only need to query locally for symbols. Meaning
        //just this enhancement module's scope, otherwise we'd get T, Max_Depth,
        //etc from the concept. We just want the ones (if any) from enhancement.
        spec.addGettersAndMembersForModuleParameterSyms(
                moduleScope.getSymbolsOfType(ModuleParameterSymbol.class));
        file.module = spec;
        built.put(ctx, file);
    }

    @Override
    public void exitConceptExtImplModuleDecl(
            ResolveParser.ConceptExtImplModuleDeclContext ctx) {
        ModuleFile file = buildFile();
        ExtensionImplModule impl =
                new ExtensionImplModule(ctx.name.getText(),
                        ctx.extension.getText(), ctx.concept.getText(), file);
        Scope conceptScope = null;
        try {
            conceptScope = symtab.getModuleScope(new ModuleIdentifier(ctx.concept));
            impl.addDelegateMethods(
                    conceptScope.getSymbolsOfType(OperationSymbol.class,
                            TypeModelSymbol.class));
        } catch (NoSuchModuleException e) {
        }
        if (ctx.implBlock() != null) {
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().operationProcedureDecl(), built));
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().procedureDecl(), built));
        }
        try {
            //first the concept spec
            List<ModuleParameterSymbol> allSymsFromConceptAndExtAndThisModule =
                    symtab.getModuleScope(new ModuleIdentifier(ctx.concept))
                            .getSymbolsOfType(ModuleParameterSymbol.class);
            //then the extension spec
            allSymsFromConceptAndExtAndThisModule.addAll(
                    symtab.getModuleScope(new ModuleIdentifier(ctx.extension))
                            .getSymbolsOfType(ModuleParameterSymbol.class));
            //now this
            allSymsFromConceptAndExtAndThisModule.addAll(moduleScope
                    .getSymbolsOfType(ModuleParameterSymbol.class));
            impl.addGettersAndMembersForModuleParameterSyms(
                    allSymsFromConceptAndExtAndThisModule);
        } catch (NoSuchModuleException e) {
        }
        impl.addCtor();
        file.module = impl;
        built.put(ctx, file);
    }

    protected ModuleFile buildFile() {
        AnnotatedModule annotatedTree = gen.getModule();
        return new ModuleFile(annotatedTree, Utils.groomFileName(annotatedTree
                .getFileName()), compiler.genPackage);
    }

    protected boolean withinFacilityModule() {
        ParseTree t = gen.getModule().getRoot();
        return t.getChild(0) instanceof ResolveParser.FacilityModuleDeclContext;
    }

    protected boolean isJavaLocallyAccessibleSymbol(Symbol s)
            throws NoSuchSymbolException {
        //System.out.println("symbol: "+s.getNameToken()+":"+s.getModuleIdentifier()+" is locally accessible?");
        boolean result = isJavaLocallyAccessibleSymbol(s.getModuleIdentifier());
        //System.out.println(result);
        return result;
    }

    protected boolean isJavaLocallyAccessibleSymbol(
            @NotNull ModuleIdentifier symbolModuleID) {
        //was s defined in the module we're translating?
        if (moduleScope.getModuleIdentifier().equals(symbolModuleID)) {
            return true;
        }
        else { //was s defined in our parent concept or enhancement?
            ParseTree thisTree = moduleScope.getDefiningTree();
            if (thisTree instanceof ResolveParser.ModuleDeclContext) {
                thisTree = thisTree.getChild(0);
            }
            if (thisTree instanceof ResolveParser.ConceptImplModuleDeclContext) {
                ResolveParser.ConceptImplModuleDeclContext asConceptImpl =
                        (ResolveParser.ConceptImplModuleDeclContext) thisTree;
                return symbolModuleID.getNameString()
                        .equals(asConceptImpl.concept.getText());
            }
            else if (thisTree instanceof ResolveParser.ConceptExtImplModuleDeclContext) {
                ResolveParser.ConceptExtImplModuleDeclContext asExtensionImpl =
                        (ResolveParser.ConceptExtImplModuleDeclContext) thisTree;
                return symbolModuleID.getNameString()
                        .equals(asExtensionImpl.concept.getText());
            }
        }
        return false;
    }

    protected CallStat buildPrimitiveInfixStat(String name,
                                               ParserRuleContext left,
                                               ParserRuleContext right) {
        NormalQualifier qualifier = new NormalQualifier("RESOLVEBase");
        return new CallStat(qualifier, name, (Expr) built.get(left),
                (Expr) built.get(right));
    }

    protected Qualifier buildQualifier(@Nullable Token refQualifier,
                                       @NotNull Token refName) {
        try {
            Symbol corresondingSym = null;
            if (refQualifier == null) {
                corresondingSym =
                        moduleScope.queryForOne(new NameQuery(null,
                                refName, true));
                NormalQualifier q;
                if (isJavaLocallyAccessibleSymbol(corresondingSym)) {
                    //this.<symName>
                    if (withinFacilityModule()) {
                        q = new NormalQualifier(
                                moduleScope.getModuleIdentifier().getNameString());
                    }
                    else {
                        q = new NormalQualifier("this");
                    }
                }
                else { //something referenced from a facility module (to say another facility module)
                    //Test_Fac.<symName>
                    q = new NormalQualifier(
                            corresondingSym.getModuleIdentifier().getNameString());
                }
                return q;
            }
            //We're here: so the call (or thing) was qualified... is the qualifier
            //referring to a facility? Let's check.
            FacilitySymbol s =
                    moduleScope.queryForOne(
                            new NameQuery(null,
                                    refQualifier, true)).toFacilitySymbol();
            //ok, it's referring to a facility alright
            //(we would've already been kicked to catch below if it wasn't).
            //So let's assign correspondingSym using a namequery with 'refqualifier'
            //as the qualifier.
            corresondingSym =
                    moduleScope.queryForOne(new NameQuery(refQualifier,
                            refName, true));
            return new FacilityQualifier(
                    corresondingSym.getModuleIdentifier().getNameString(), s.getName());
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            //Todo: symQualifier can be null here -- npe waiting to happen. Address this.
            assert refQualifier != null;
            if (isJavaLocallyAccessibleSymbol(new ModuleIdentifier(refQualifier))) {
                return new NormalQualifier("this");
            }
            return new NormalQualifier(refQualifier.getText());
        } catch (UnexpectedSymbolException | NoSuchModuleException e) {
            throw new RuntimeException();//populator should've tripped it.
        }
    }
}