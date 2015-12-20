package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.*;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.rsrg.semantics.*;

public class ModelBuilder extends ResolveBaseListener {

    public ParseTreeProperty<OutputModelObject> built =
            new ParseTreeProperty<>();
    //private final ModuleScopeBuilder moduleScope;
    private final JavaCodeGenerator gen;
    private final MathSymbolTable symtab;
    private final AnnotatedModule tr;

    public ModelBuilder(JavaCodeGenerator gen, MathSymbolTable symtab) {
        this.gen = gen;
        this.symtab = symtab;
        this.tr = gen.getModule();
    }

  /*  @Override public void exitTypeModelDecl(ResolveParser.TypeModelDeclContext ctx) {
        built.put(ctx, new TypeInterfaceDef(ctx.name.getText()));
    }

    @Override public void exitOperationDecl(ResolveParser.OperationDeclContext ctx) {
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

    @Override public void exitProcedureDecl(ResolveParser.ProcedureDeclContext ctx) {
        FunctionImpl f =
                buildFunctionImpl(ctx.name.getText(),
                        ctx.type(), ctx.operationParameterList()
                                .parameterDeclGroup(), ctx.variableDeclGroup(),
                        ctx.stmt());
        f.implementsOper = true;
        built.put(ctx, f);
    }

    protected FunctionImpl buildFunctionImpl(String name,
                                             ResolveParser.TypeContext type,
                                             List<ResolveParser.ParameterDeclGroupContext> paramGroupings,
                                             List<ResolveParser.VariableDeclGroupContext> variableGroupings,
                                             List<ResolveParser.StmtContext> stats) {
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
            if ( s.assignStmt() != null
                    && s.assignStmt().left.getText().equals(name)
                    && f.hasReturn ) {
                Expr rhs = (Expr) built.get(s.assignStmt().right);
                f.vars.add(new VariableDef(name, rhs));
                f.stats.add(new ReturnStat(name));
            }
        }
        return f;
    }

    @Override public void exitFacilityDecl(ResolveParser.FacilityDeclContext ctx) {
        FacilityDef f = new FacilityDef(ctx.name.getText(), ctx.spec.getText());
        f.isStatic = withinFacilityModule();
        List<DecoratedFacilityInstantiation> layers = new ArrayList<>();

        DecoratedFacilityInstantiation basePtr =
                new DecoratedFacilityInstantiation(ctx.spec.getText(),
                        ctx.impl.getText());
        basePtr.isProxied = false;
        for (ResolveParser.TypeContext t : ctx.type()) {
            try {
                Symbol s =
                        moduleScope.queryForOne(new NameQuery(t.qualifier,
                                t.name, true));
                if ( s instanceof GenericSymbol) {
                    basePtr.args.add(new MethodCall((TypeInit) built.get(t)));
                }
                else {
                    basePtr.args.add((TypeInit) built.get(t));
                }
            }
            catch (NoSuchSymbolException | DuplicateSymbolException e) {
                e.printStackTrace();
            }
        }
        List<Expr> specArgs =
                ctx.specArgs == null ? new ArrayList<>() : Utils.collect(
                        Expr.class, ctx.specArgs.moduleArgument(), built);
        List<Expr> implArgs =
                ctx.implArgs == null ? new ArrayList<>() : Utils.collect(
                        Expr.class, ctx.implArgs.moduleArgument(), built);
        basePtr.args.addAll(specArgs);
        basePtr.args.addAll(implArgs);

        for (ResolveParser.EnhancementPairDeclContext pair : ctx.enhancementPairDecl()) {
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
        }
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

    @Override public void exitType(ResolveParser.TypeContext ctx) {
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
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            exemplarName = ctx.name.getText().substring(0, 1); //default name
        }
        representationClass.isStatic = withinFacilityModule();
        representationClass.referredToByExemplar = exemplarName;
        if ( ctx.record() != null ) {
            for (ResolveParser.RecordVariableDeclGroupContext grp : ctx
                    .record().recordVariableDeclGroup()) {
                representationClass.fields.addAll(Utils.collect(
                        VariableDef.class, grp.ID(), built));
            }
        }
        if ( ctx.typeImplInit() != null ) {
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

    @Override public void exitModule(ResolveParser.ModuleContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitModuleArgument(
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
                e =
                        new AnonOpParameterClassInstance(buildQualifier(
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
    }

    private Symbol findSymbolFor(Token qualifier, Token name) {
        try {
            return symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(qualifier, name, true));
        } catch (NoSuchSymbolException|DuplicateSymbolException e) {
            throw new RuntimeException();//shouldn't happen
        }
    }

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
        built.put(ctx, new CallStat((Expr) built.get(ctx.progExp())));
    }

    @Override public void exitWhileStmt(ResolveParser.WhileStmtContext ctx) {
        WhileStat w = new WhileStat((Expr) built.get(ctx.progExp()));
        w.stats.addAll(Utils.collect(Stat.class, ctx.stmt(), built));
        built.put(ctx, w);
    }

    @Override public void exitIfStmt(ResolveParser.IfStmtContext ctx) {
        IfStat i = new IfStat((Expr) built.get(ctx.progExp()));
        i.ifStats.addAll(Utils.collect(Stat.class, ctx.stmt(), built));
        if ( ctx.elsePart() != null ) {
            i.elseStats.addAll(Utils.collect(Stat.class,
                    ctx.elsePart().stmt(), built));
        }
        built.put(ctx, i);
    }

    @Override public void exitProgNestedExp(ResolveParser.ProgNestedExpContext ctx) {
        built.put(ctx, built.get(ctx.progExp()));
    }

    @Override public void exitProgPrimaryExp(ResolveParser.ProgPrimaryExpContext ctx) {
        built.put(ctx, built.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(ResolveParser.ProgPrimaryContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitProgParamExp(ResolveParser.ProgParamExpContext ctx) {
        List<Expr> args = Utils.collect(Expr.class, ctx.progExp(), built);
        if ( referencesOperationParameter(ctx.name.getText()) ) {
            built.put(ctx, new MethodCall.OperationParameterMethodCall(
                    ctx.name.getText(), args));
        }
        else {
            built.put(ctx, new MethodCall(buildQualifier(ctx.qualifier,
                    ctx.name), ctx.name.getText(), args));
        }
    }

    private boolean referencesOperationParameter(String name) {
        return !moduleScope.getSymbolsOfType(OperationSymbol.class).stream()
                .filter(f -> f.getNameToken().equals(name))
                .filter(OperationSymbol::isModuleParameter)
                .collect(Collectors.toList())
                .isEmpty();
    }

    @Override public void exitProgUnaryExp(ResolveParser.ProgUnaryExpContext ctx) {
        if (ctx.NOT() != null) {
            built.put(ctx, buildSugaredProgExp(ctx, ctx.op, ctx.progExp()));
        }
        else {
            Token qualifier =
                    Utils.createTokenFrom(ctx.getStart(), "Std_Integer_Fac");
            Token name = Utils.createTokenFrom(ctx.getStart(), "Negate");
            built.put(ctx, new MethodCall(buildQualifier(qualifier, name),
                    name.getText(), (Expr) built.get(ctx.progExp())));
        }
    }

    @Override public void exitProgPostfixExp(
            ResolveParser.ProgPostfixExpContext ctx) {
        built.put(ctx, buildSugaredProgExp(ctx, ctx.op, ctx.progExp()));
    }

    @Override public void exitProgInfixExp(ResolveParser.ProgInfixExpContext ctx) {
        built.put(ctx, buildSugaredProgExp(ctx, ctx.op, ctx.progExp()));
    }

    private MethodCall buildSugaredProgExp(ParserRuleContext ctx, Token op,
                                           ParseTree... args) {
        return buildSugaredProgExp(ctx, op, Arrays.asList(args));
    }

    private MethodCall buildSugaredProgExp(ParserRuleContext ctx, Token op,
                                           List<? extends ParseTree> args) {
        List<PTType> argTypes = args.stream().map(tr.progTypes::get)
                .collect(Collectors.toList());
        HardCodedProgOps.BuiltInOpAttributes o =
                HardCodedProgOps.convert(op, argTypes);
        return new MethodCall(buildQualifier(o.qualifier, o.name),
                o.name.getText(), Utils.collect(Expr.class, args, built));
    }

    @Override public void exitProgVarExp(ResolveParser.ProgVarExpContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitProgNamedExp(ResolveParser.ProgNamedExpContext ctx) {
        built.put(ctx, new VarNameRef(new NormalQualifier("this"),
                ctx.name.getText()));
    }

    @Override public void exitProgMemberExp(ResolveParser.ProgMemberExpContext ctx) {
        List<MemberRef> refs = ctx.ID()
                .stream()
                .map(t -> new MemberRef(t.getText(), tr.progTypes.get(t)))
                .collect(Collectors.toList());
        Collections.reverse(refs);
        refs.add(new MemberRef(ctx.progNamedExp().name.getText(),
                tr.progTypes.get(ctx.progNamedExp())));

        for (int i = 0; i < refs.size(); i++) {
            refs.get(i).isLastRef = i==0;
            if (i + 1 < refs.size()) {
                refs.get(i).child = refs.get(i + 1);
            } else {
                refs.get(i).isBaseRef = true;
            }
        }
        built.put(ctx, refs.get(0));
    }

    @Override public void exitProgBooleanLiteralExp(
            ResolveParser.ProgBooleanLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier("Boolean_Template",
                "Std_Boolean_Fac"), "Boolean", ctx.getText()));
    }

    @Override public void exitProgIntegerLiteralExp(
            ResolveParser.ProgIntegerLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier("Integer_Template",
                "Std_Integer_Fac"), "Integer", ctx.getText()));
    }

    @Override public void exitProgCharacterLiteralExp(
            ResolveParser.ProgCharacterLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier("Character_Template",
                "Std_Character_Fac"), "Character", ctx.getText()));
    }

    @Override public void exitProgStringLiteralExp(
            ResolveParser.ProgStringLiteralExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier("Char_Str_Template",
                "Std_Char_Str_Fac"), "Char_Str", ctx.getText()));
    }

    @Override public void exitConceptImplModule(
            ResolveParser.ConceptImplModuleContext ctx) {
        ModuleFile file = buildFile();
        ConceptImplModule impl =
                new ConceptImplModule(ctx.name.getText(),
                        ctx.concept.getText(), file);
        if ( ctx.implBlock() != null ) {
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().procedureDecl(), built));
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().operationProcedureDecl(), built));
            impl.repClasses.addAll(Utils.collect(MemberClassDef.class, ctx
                    .implBlock().typeRepresentationDecl(), built));
            impl.facilityVars.addAll(Utils.collect(FacilityDef.class, ctx
                    .implBlock().facilityDecl(), built));
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
    }

    @Override public void exitFacilityModule(ResolveParser.FacilityModuleContext ctx) {
        ModuleFile file = buildFile();
        FacilityImplModule impl =
                new FacilityImplModule(ctx.name.getText(), file);

        if ( ctx.facilityBlock() != null ) {
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

    @Override public void exitConceptModule(ResolveParser.ConceptModuleContext ctx) {
        ModuleFile file = buildFile();
        SpecModule spec = new SpecModule.ConceptModule(ctx.name.getText(), file);

        if ( ctx.conceptBlock() != null ) {
            spec.types.addAll(Utils.collect(TypeInterfaceDef.class, ctx
                    .conceptBlock().typeModelDecl(), built));
            spec.funcs.addAll(Utils.collect(FunctionDef.class, ctx
                    .conceptBlock().operationDecl(), built));
        }
        spec.addGettersAndMembersForModuleParameterizableSyms(moduleScope
                .query(new SymbolTypeQuery<>(Symbol.class)));

        file.module = spec;
        built.put(ctx, file);
    }

    @Override public void exitEnhancementModule(
            ResolveParser.EnhancementModuleContext ctx) {
        ModuleFile file = buildFile();
        SpecModule spec = new SpecModule.EnhancementModule(ctx.name.getText(),
                ctx.concept.getText(), file);

        if ( ctx.enhancementBlock() != null ) {
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

    @Override public void exitEnhancementImplModule(
            ResolveParser.EnhancementImplModuleContext ctx) {
        ModuleFile file = buildFile();
        EnhancementImplModule impl =
                new EnhancementImplModule(ctx.name.getText(),
                        ctx.enhancement.getText(), ctx.concept.getText(), file);
        Scope conceptScope = symtab.moduleScopes.get(ctx.concept.getText());
        impl.addDelegateMethods(
                conceptScope.getSymbolsOfType(OperationSymbol.class,
                        TypeModelSymbol.class));
        if ( ctx.implBlock() != null ) {
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
    }

    protected ModuleFile buildFile() {
        AnnotatedTree annotatedTree = gen.getModule();
        return new ModuleFile(annotatedTree, Utils.groomFileName(annotatedTree
                .getFileName()));
    }

    protected boolean withinFacilityModule() {
        ParseTree t = gen.getModule().getRoot();
        return t.getChild(0) instanceof ResolveParser.FacilityModuleContext;
    }

    protected boolean isJavaLocallyAccessibleSymbol(Symbol s)
            throws NoSuchSymbolException {
        //System.out.println("symbol: "+s.getNameToken()+":"+s.getModuleIdentifier()+" is locally accessible?");
        boolean result = isJavaLocallyAccessibleSymbol(s.getModuleIdentifier());
        //System.out.println(result);
        return result;
    }

    protected boolean isJavaLocallyAccessibleSymbol(String symbolModuleID) {
        //was s defined in the module we're translating?
        if ( moduleScope.getModuleIdentifier().equals(symbolModuleID) ) {
            return true;
        }
        else { //was s defined in our parent concept or enhancement?
            ParseTree thisTree = moduleScope.getDefiningTree();
            if (thisTree instanceof ResolveParser.ModuleContext) {
                thisTree = thisTree.getChild(0);
            }
            if ( thisTree instanceof ResolveParser.ConceptImplModuleContext ) {
                ResolveParser.ConceptImplModuleContext asConceptImpl =
                        (ResolveParser.ConceptImplModuleContext) thisTree;
                return symbolModuleID.equals(asConceptImpl.concept.getText());
            }
            else if ( thisTree instanceof ResolveParser.EnhancementImplModuleContext ) {
                ResolveParser.EnhancementImplModuleContext asEnhancementImpl =
                        (ResolveParser.EnhancementImplModuleContext) thisTree;
                return symbolModuleID.equals(asEnhancementImpl.concept.getText());
            }
        }
        return false;
    }

    protected CallStat buildPrimitiveInfixStat(String name,
                                               ParserRuleContext left,
                                               ParserRuleContext right) {
        Qualifier.NormalQualifier qualifier = new NormalQualifier("RESOLVEBase");
        return new CallStat(qualifier, name, (Expr) built.get(left),
                (Expr) built.get(right));
    }

    protected Qualifier buildQualifier(Token refQualifier, Token refName) {
        try {
            Symbol corresondingSym = null;
            if ( refQualifier == null ) {
                corresondingSym =
                        moduleScope.queryForOne(new NameQuery(null,
                                refName, true));
                Qualifier.NormalQualifier q;
                if ( isJavaLocallyAccessibleSymbol(corresondingSym) ) {
                    //this.<symName>
                    if ( withinFacilityModule() ) {
                        q = new Qualifier.NormalQualifier(moduleScope.getModuleIdentifier());
                    }
                    else {
                        q = new Qualifier.NormalQualifier("this");
                    }
                }
                else {
                    //Test_Fac.<symName>
                    q = new Qualifier.NormalQualifier(
                            corresondingSym.getModuleIdentifier());
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
            return new Qualifier.FacilityQualifier(
                    corresondingSym.getModuleIdentifier(), s.getNameToken());
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            //Todo: symQualifier can be null here -- npe waiting to happen. Address this.
            assert refQualifier != null;
            if ( isJavaLocallyAccessibleSymbol(refQualifier.getText()) ) {
                return new Qualifier.NormalQualifier("this");
            }
            return new Qualifier.NormalQualifier(refQualifier.getText());
        }
        catch (UnexpectedSymbolException use) {
            throw new RuntimeException(); //should've been caught looong ago.
        }
    }*/
}