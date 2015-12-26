package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.*;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.HardCodedProgOps;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.PTNamed;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.query.NameQuery;
import org.rsrg.semantics.query.SymbolTypeQuery;
import org.rsrg.semantics.query.UnqualifiedNameQuery;
import org.rsrg.semantics.symbol.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static edu.clemson.resolve.codegen.model.Qualifier.*;
import static edu.clemson.resolve.codegen.model.Stat.*;

public class ModelBuilder extends ResolveBaseListener {

    public ParseTreeProperty<OutputModelObject> built =
            new ParseTreeProperty<>();

    private final JavaCodeGenerator gen;
    private final MathSymbolTable symtab;

    @NotNull private final AnnotatedModule tr;
    @NotNull private final RESOLVECompiler compiler;

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

    @Override public void exitModuleDecl(ResolveParser.ModuleDeclContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitTypeModelDecl(
            ResolveParser.TypeModelDeclContext ctx) {
        built.put(ctx, new TypeInterfaceDef(ctx.name.getText()));
    }

    @Override public void exitOperationDecl(
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

    @Override public void exitOperationProcedureDecl(
            ResolveParser.OperationProcedureDeclContext ctx) {
        FunctionImpl f =
                buildFunctionImpl(ctx.name.getText(),
                        ctx.type(), ctx.operationParameterList()
                                .parameterDeclGroup(), ctx.variableDeclGroup(),
                        ctx.stmt());
        built.put(ctx, f);
    }

    @Override public void exitProcedureDecl(
            ResolveParser.ProcedureDeclContext ctx) {
        FunctionImpl f =
                buildFunctionImpl(ctx.name.getText(),
                        ctx.type(), ctx.operationParameterList()
                                .parameterDeclGroup(), ctx.variableDeclGroup(),
                        ctx.stmt());
        f.implementsOper = true;
        built.put(ctx, f);
    }

    protected FunctionImpl buildFunctionImpl(@NotNull String name,
                                             @Nullable ResolveParser.TypeContext type,
                                             @NotNull List<ResolveParser.ParameterDeclGroupContext> paramGroupings,
                                             @NotNull List<ResolveParser.VariableDeclGroupContext> variableGroupings,
                                             @NotNull List<ResolveParser.StmtContext> stats) {
        FunctionImpl f = new FunctionImpl(name);
        f.hasReturn = type != null;
        f.isStatic = withinFacilityModule();
        for (ResolveParser.ParameterDeclGroupContext grp : paramGroupings) {
            f.params.addAll(Utils.collect(ParameterDef.class, grp.ID(), built));
        }
        for (ResolveParser.VariableDeclGroupContext grp : variableGroupings) {
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

    @Override public void exitFacilityDecl(
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
        //List<Expr> implArgs =
        //        ctx.implArgs == null ? new ArrayList<>() : Utils.collect(
        //                Expr.class, ctx.implArgs.moduleArgument(), built);
        basePtr.args.addAll(specArgs);
        //basePtr.args.addAll(implArgs);

        /*for (ResolveParser.EnhancementPairDeclContext pair : ctx.enhancementPairDecl()) {
            DecoratedFacilityInstantiation layer =
                    new DecoratedFacilityInstantiation(pair.spec.getText(),
                            pair.impl.getText());
            specArgs = pair.specArgs == null ? new ArrayList<>() : Utils.collect(
                            Expr.class, pair.specArgs.moduleArgument(), built);
            implArgs = pair.implArgs == null ? new ArrayList<>() : Utils.collect(
                            Expr.class, pair.implArgs.moduleArgument(), built);
            layer.args.addAll(basePtr.args); // always prefaced with the base facility args
            layer.args.addAll(specArgs);
            layer.args.addAll(implArgs);
            layers.add(layer);
        }

        for (int i = 0; i < layers.size(); i++) {
            layers.get(i).isProxied = ctx.enhancementPairDecl().size() > 1;
            if ( i + 1 < layers.size() ) {
                layers.get(i).child = layers.get(i + 1);
            }
            else {
                layers.get(i).child = basePtr;
            }
        }*/
        f.root = layers.isEmpty() ? basePtr : layers.get(0);
        built.put(ctx, f);
    }

    @Override public void exitVariableDeclGroup(
            ResolveParser.VariableDeclGroupContext ctx) {
        Expr init = (Expr) built.get(ctx.type());
        for (TerminalNode t : ctx.ID()) {
            //System.out.println("adding "+t.getText()+" to built map");
            built.put(t, new VariableDef(t.getSymbol().getText(), init));
        }
    }

    @Override public void exitRecordVariableDeclGroup(
            ResolveParser.RecordVariableDeclGroupContext ctx) {
        Expr init = (Expr) built.get(ctx.type());
        for (TerminalNode t : ctx.ID()) {
            //System.out.println("adding "+t.getText()+" to built map");
            built.put(t, new VariableDef(t.getSymbol().getText(), init));
        }
    }

    @Override public void exitNamedType(ResolveParser.NamedTypeContext ctx) {
        built.put(ctx, new TypeInit(buildQualifier(
                ctx.qualifier, ctx.name), ctx.name.getText(), ""));
    }

    @Override public void exitTypeRepresentationDecl(
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
                    ((PTNamed) x.toProgTypeSymbol().getProgramType())
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
            for (ResolveParser.RecordVariableDeclGroupContext grp :
                    typeAsRecord.recordVariableDeclGroup()) {
                representationClass.fields.addAll(Utils.collect(
                        VariableDef.class, grp.ID(), built));
            }
        }
        if (ctx.typeImplInit() != null) {
            representationClass.initVars.addAll(Utils.collect(
                    VariableDef.class, ctx.typeImplInit().variableDeclGroup(),
                    built));
            representationClass.initStats.addAll(Utils.collect(Stat.class, ctx
                    .typeImplInit().stmt(), built));
        }
        built.put(ctx, representationClass);
    }

    @Override public void exitParameterDeclGroup(
            ResolveParser.ParameterDeclGroupContext ctx) {
        for (TerminalNode t : ctx.ID()) {
            built.put(t, new ParameterDef(t.getText()));
        }
    }

    /*@Override public void exitModuleArgument(
            ResolveParser.ModuleArgumentContext ctx) {
        Expr e = (Expr) built.get(ctx.progExp());
        if ( e instanceof VarNameRef ) {
            //If this is true, then we're likely dealing with a math definition
            //(which has no sensible prog type), so we ignore the rest of our
            //logic here
            //Yes, kind of weird, but then again, so is passing defs as params...
            if (tr.progTypes.get(ctx.progExp()) == null) return;

            //Todo: I think it's ok to do getChild(0) here; we know we're
            //dealing with a VarNameRef (so our (2nd) child ctx must be progNamedExp)...
            //Todo2: this line below is pretty fugly. Change me eventually.
            ResolveParser.ProgNamedExpContext argAsNamedExp =
                    (ResolveParser.ProgNamedExpContext) ctx.progExp()
                            .getChild(0).getChild(0).getChild(0);
            try {
                OperationSymbol s =
                        moduleScope.queryForOne(
                                new NameQuery(argAsNamedExp.qualifier,
                                        argAsNamedExp.name, true))
                                .toOperationSymbol();
                e = new AnonOpParameterClassInstance(buildQualifier(
                                argAsNamedExp.qualifier, argAsNamedExp.name), s);
            }
            catch (UnexpectedSymbolException use) {
                e = new VarNameRef(null, "get" + argAsNamedExp.name.getText() + "()");
            }
            catch (NoSuchSymbolException | DuplicateSymbolException e1) {
                e1.printStackTrace();
            }
        }
        built.put(ctx, e);
    }*/

    @Override public void exitStmt(ResolveParser.StmtContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitAssignStmt(ResolveParser.AssignStmtContext ctx) {
        built.put(ctx, buildPrimitiveInfixStat("assign", ctx.left, ctx.right));
    }

    @Override public void exitSwapStmt(ResolveParser.SwapStmtContext ctx) {
        built.put(ctx, buildPrimitiveInfixStat("swap", ctx.left, ctx.right));
    }

    @Override public void exitCallStmt(ResolveParser.CallStmtContext ctx) {
        built.put(ctx, new CallStat((Expr) built.get(ctx.progParamExp())));
    }

    @Override public void exitWhileStmt(
            ResolveParser.WhileStmtContext ctx) {
        WhileStat w = new WhileStat((Expr) built.get(ctx.progExp()));
        w.stats.addAll(Utils.collect(Stat.class, ctx.stmt(), built));
        built.put(ctx, w);
    }

    @Override public void exitIfStmt(
            ResolveParser.IfStmtContext ctx) {
        IfStat i = new IfStat((Expr) built.get(ctx.progExp()));
        i.ifStats.addAll(Utils.collect(Stat.class, ctx.stmt(), built));
        if (ctx.elseStmt() != null) {
            i.elseStats.addAll(Utils.collect(Stat.class,
                    ctx.elseStmt().stmt(), built));
        }
        built.put(ctx, i);
    }

    @Override public void exitProgNestedExp(
            ResolveParser.ProgNestedExpContext ctx) {
        built.put(ctx, built.get(ctx.progExp()));
    }

    @Override public void exitProgPrimaryExp(
            ResolveParser.ProgPrimaryExpContext ctx) {
        built.put(ctx, built.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            ResolveParser.ProgPrimaryContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitProgParamExp(
            ResolveParser.ProgParamExpContext ctx) {
        List<Expr> args = Utils.collect(Expr.class, ctx.progExp(), built);
        if (referencesOperationParameter(ctx.progNamedExp().name.getText())) {
            built.put(ctx, new MethodCall.OperationParameterMethodCall(
                    ctx.progNamedExp().name.getText(), args));
        } else {
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

    @Override public void exitProgInfixExp(
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
        List<PTType> argTypes = args.stream().map(tr.progTypes::get)
                .collect(Collectors.toList());
        HardCodedProgOps.BuiltInOpAttributes o =
                HardCodedProgOps.convert(op, argTypes);
        return new MethodCall(buildQualifier(o.qualifier, o.name),
                o.name.getText(), Utils.collect(Expr.class, args, built));
    }

    @Override public void exitProgNamedExp(
            ResolveParser.ProgNamedExpContext ctx) {
        if (Utils.getFirstAncestorOfType(ctx,
                ResolveParser.ModuleArgumentListContext.class) != null) {
            built.put(ctx, createFacilityArgumentModel(ctx));
        }
        else {
            built.put(ctx, new VarNameRef(new NormalQualifier("this"),
                    ctx.name.getText()));
        }
    }

    /** Given an arbitrary expression within some
     *  {@link ResolveParser.ModuleArgumentListContext}, returns an {@link OutputModelObject}
     *  suitable for that argument.
     */
    @NotNull private OutputModelObject createFacilityArgumentModel(
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
            throw new RuntimeException();//shouldn't happen
        }
        return result;
    }

   // private OutputModelObject buildWrapperModelForFacilityArg(
   //         @NotNull ResolveParser.ProgNamedExpContext ctx) {
   // }

    @Override public void exitProgBooleanLiteralExp(
            ResolveParser.ProgBooleanLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier(
                "Boolean_Template", "Std_Bools"), "Boolean", ctx.getText()));
    }

    @Override public void exitProgIntegerLiteralExp(
            ResolveParser.ProgIntegerLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier(
                "Integer_Template", "Std_Ints"), "Integer", ctx.getText()));
    }

    @Override public void exitProgCharacterLiteralExp(
            ResolveParser.ProgCharacterLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier(
                "Character_Template", "Std_Chars"), "Character", ctx.getText()));
    }

    @Override public void exitProgStringLiteralExp(
            ResolveParser.ProgStringLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier(
                "Char_String_Template", "Std_Char_Strings"), "Char_Str", ctx.getText()));
    }

    @Override public void exitConceptImplModuleDecl(
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
        List<Symbol> allSymsFromConceptAndImpl = null;
        try {
            allSymsFromConceptAndImpl =
                    symtab.getModuleScope(new ModuleIdentifier(ctx.concept))
                            .getSymbolsOfType(Symbol.class);
            allSymsFromConceptAndImpl.addAll(moduleScope
                    .getSymbolsOfType(Symbol.class));
            impl.addGettersAndMembersForModuleParameterizableSyms(
                    allSymsFromConceptAndImpl);
        } catch (NoSuchModuleException e) { //shouldn't happen
        }
        impl.addCtor();
        file.module = impl;
        built.put(ctx, file);
    }

    @Override public void exitFacilityModuleDecl(
            ResolveParser.FacilityModuleDeclContext ctx) {
        ModuleFile file = buildFile();
        FacilityImplModule impl =
                new FacilityImplModule(ctx.name.getText(), file);

        if (ctx.facilityBlock() != null) {
            impl.facilities.addAll(Utils.collect(FacilityDef.class, ctx
                    .facilityBlock().facilityDecl(), built));
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .facilityBlock().operationProcedureDecl(), built));
            //impl.repClasses.addAll(Utils.collect(MemberClassDef.class, ctx
            //        .facilityBlock().typeRepresentationDecl(), built));
        }
        file.module = impl;
        built.put(ctx, file);
    }

    @Override public void exitConceptModuleDecl(
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
            spec.addGettersAndMembersForModuleParameterizableSyms(moduleScope
                    .query(new SymbolTypeQuery<>(Symbol.class)));
        } catch (NoSuchModuleException|UnexpectedSymbolException e) {
        }
        file.module = spec;
        built.put(ctx, file);
    }

    /*@Override
    public void exitEnhancementModule(
            ResolveParser.EnhancementModuleContext ctx) {
        ModuleFile file = buildFile();
        SpecModule spec = new SpecModule.EnhancementModule(ctx.name.getText(),
                ctx.concept.getText(), file);

        if (ctx.enhancementBlock() != null) {
            spec.types.addAll(Utils.collect(TypeInterfaceDef.class, ctx
                    .enhancementBlock().typeModelDecl(), built));
            spec.funcs.addAll(Utils.collect(FunctionDef.class, ctx
                    .enhancementBlock().operationDecl(), built));
        }
        //Note that here we only need to query locally for symbols. Meaning
        //just this enhancement module's scope, otherwise we'd get T, Max_Depth,
        //etc from the concept. We just want the ones (if any) from enhancement.
        spec.addGettersAndMembersForModuleParameterizableSyms(
                moduleScope.getSymbolsOfType(Symbol.class));
        file.module = spec;
        built.put(ctx, file);
    }

    @Override
    public void exitEnhancementImplModule(
            ResolveParser.EnhancementImplModuleContext ctx) {
        ModuleFile file = buildFile();
        EnhancementImplModule impl =
                new EnhancementImplModule(ctx.name.getText(),
                        ctx.enhancement.getText(), ctx.concept.getText(), file);
        Scope conceptScope = symtab.moduleScopes.get(ctx.concept.getText());
        impl.addDelegateMethods(
                conceptScope.getSymbolsOfType(OperationSymbol.class,
                        TypeModelSymbol.class));
        if (ctx.implBlock() != null) {
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().operationProcedureDecl(), built));
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().procedureDecl(), built));
        }
        List<Symbol> allSymsFromConceptAndImpl = symtab.moduleScopes.get(
                ctx.concept.getText()).getSymbolsOfType(Symbol.class);
        allSymsFromConceptAndImpl.addAll(moduleScope
                .getSymbolsOfType(Symbol.class));
        impl.addGettersAndMembersForModuleParameterizableSyms(
                allSymsFromConceptAndImpl);

        impl.addCtor();
        file.module = impl;
        built.put(ctx, file);
    }*/

    protected ModuleFile buildFile() {
        AnnotatedModule annotatedTree = gen.getModule();
        return new ModuleFile(annotatedTree, Utils.groomFileName(annotatedTree
                .getFileName()));
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
        } else { //was s defined in our parent concept or enhancement?
            ParseTree thisTree = moduleScope.getDefiningTree();
            if (thisTree instanceof ResolveParser.ModuleDeclContext) {
                thisTree = thisTree.getChild(0);
            }
            if (thisTree instanceof ResolveParser.ConceptImplModuleDeclContext) {
                ResolveParser.ConceptImplModuleDeclContext asConceptImpl =
                        (ResolveParser.ConceptImplModuleDeclContext) thisTree;
                return symbolModuleID.getNameString()
                        .equals(asConceptImpl.concept.getText());
            } /*else if (thisTree instanceof ResolveParser.EnhancementImplModuleContext) {
                ResolveParser.EnhancementImplModuleContext asEnhancementImpl =
                        (ResolveParser.EnhancementImplModuleContext) thisTree;
                return symbolModuleID.equals(asEnhancementImpl.concept.getText());
            }*/
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
                    } else {
                        q = new NormalQualifier("this");
                    }
                } else { //something referenced from a facility module (to say another facility module)
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
        } catch (UnexpectedSymbolException|NoSuchModuleException e) {
            throw new RuntimeException();//populator should've tripped it.
        }
    }
}