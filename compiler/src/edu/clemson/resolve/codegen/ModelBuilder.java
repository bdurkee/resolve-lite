package edu.clemson.resolve.codegen;

import edu.clemson.resolve.codegen.model.*;
import edu.clemson.resolve.codegen.model.Qualifier.NormalQualifier;
import edu.clemson.resolve.codegen.model.Qualifier.FacilityQualifier;
import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveLexer;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.rsrg.semantics.*;
import org.rsrg.semantics.programtype.PTNamed;
import org.rsrg.semantics.query.NameQuery;
import org.rsrg.semantics.query.SymbolTypeQuery;
import org.rsrg.semantics.query.UnqualifiedNameQuery;
import org.rsrg.semantics.symbol.*;

import java.util.*;
import java.util.stream.Collectors;

public class ModelBuilder extends ResolveBaseListener {

    public ParseTreeProperty<OutputModelObject> built =
            new ParseTreeProperty<>();
    private final ModuleScopeBuilder moduleScope;
    private final CodeGenerator gen;
    private final SymbolTable symtab;
    private final AnnotatedTree tr;

    public ModelBuilder(@NotNull CodeGenerator g, @NotNull SymbolTable symtab) {
        this.gen = g;
        this.moduleScope = symtab.moduleScopes.get(g.getModule().getName());
        this.symtab = symtab;
        this.tr = g.getModule();
    }

    @Override public void exitTypeModelDecl(
            @NotNull Resolve.TypeModelDeclContext ctx) {
        built.put(ctx, new TypeInterfaceDef(ctx.name.getText()));
    }

    @Override public void exitOperationDecl(
            @NotNull Resolve.OperationDeclContext ctx) {
        FunctionDef f = new FunctionDef(ctx.name.getText());
        f.hasReturn = ctx.type() != null;
        f.isStatic = withinFacilityModule();
        for (Resolve.ParameterDeclGroupContext grp : ctx
                .operationParameterList().parameterDeclGroup()) {
            for (TerminalNode id : grp.ID()) {
                f.params.add((ParameterDef) built.get(id));
            }
        }
        built.put(ctx, f);
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull Resolve.OperationProcedureDeclContext ctx) {
        FunctionImpl f =
                buildFunctionImpl(ctx.name.getText(),
                        ctx.type(), ctx.operationParameterList()
                                .parameterDeclGroup(), ctx.variableDeclGroup(),
                        ctx.stmtBlock() != null ? ctx.stmtBlock().stmt() :
                                new ArrayList<Resolve.StmtContext>());
        built.put(ctx, f);
    }

    @Override public void exitProcedureDecl(
            @NotNull Resolve.ProcedureDeclContext ctx) {
        FunctionImpl f =
                buildFunctionImpl(ctx.name.getText(),
                        ctx.type(), ctx.operationParameterList()
                                .parameterDeclGroup(), ctx.variableDeclGroup(),
                        ctx.stmtBlock() != null ? ctx.stmtBlock().stmt() :
                                new ArrayList<Resolve.StmtContext>());
        f.implementsOper = true;
        built.put(ctx, f);
    }

    protected FunctionImpl buildFunctionImpl(String name,
                     Resolve.TypeContext type,
                     List<Resolve.ParameterDeclGroupContext> paramGroupings,
                     List<Resolve.VariableDeclGroupContext> variableGroupings,
                     List<Resolve.StmtContext> stats) {
        FunctionImpl f = new FunctionImpl(name);
        f.hasReturn = type != null;
        f.isStatic = withinFacilityModule();
        for (Resolve.ParameterDeclGroupContext grp : paramGroupings) {
            f.params.addAll(Utils.collect(ParameterDef.class, grp.ID(), built));
        }
        for (Resolve.VariableDeclGroupContext grp : variableGroupings) {
            f.vars.addAll(Utils.collect(VariableDef.class, grp.ID(), built));
        }
        for (Resolve.StmtContext s : stats) {
            f.stats.add((Stat) built.get(s));
            //Resolve returns are buried in an assignment
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

    @Override public void exitFacilityDecl(
            @NotNull Resolve.FacilityDeclContext ctx) {
        FacilityDef f = new FacilityDef(ctx.name.getText(), ctx.spec.getText());
        f.isStatic = withinFacilityModule();
        List<DecoratedFacilityInstantiation> layers = new ArrayList<>();

        DecoratedFacilityInstantiation basePtr =
                new DecoratedFacilityInstantiation(ctx.spec.getText(),
                        ctx.impl.getText());
        basePtr.isProxied = false;
        for (Resolve.TypeContext t : ctx.type()) {
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

        for (Resolve.EnhancementPairDeclContext pair : ctx.enhancementPairDecl()) {
            DecoratedFacilityInstantiation layer =
                    new DecoratedFacilityInstantiation(pair.spec.getText(),
                            pair.impl.getText());
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
            @NotNull Resolve.VariableDeclGroupContext ctx) {
        Expr init = (Expr) built.get(ctx.type());
        for (TerminalNode t : ctx.ID()) {
            //System.out.println("adding "+t.getText()+" to built map");
            built.put(t, new VariableDef(t.getSymbol().getText(), init));
        }
    }

    @Override public void exitRecordVariableDeclGroup(
            @NotNull Resolve.RecordVariableDeclGroupContext ctx) {
        Expr init = (Expr) built.get(ctx.type());
        for (TerminalNode t : ctx.ID()) {
            //System.out.println("adding "+t.getText()+" to built map");
            built.put(t, new VariableDef(t.getSymbol().getText(), init));
        }
    }

    @Override public void exitType(@NotNull Resolve.TypeContext ctx) {
        built.put(ctx, new TypeInit(buildQualifier(ctx.qualifier, ctx.name),
                ctx.name.getText(), ""));
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull Resolve.TypeRepresentationDeclContext ctx) {
        MemberClassDef representationClass =
                new MemberClassDef(ctx.name.getText());
        String exemplarName = "";
        try {
            //Maybe in the future we can assign program types to the ctxs?
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
            for (Resolve.RecordVariableDeclGroupContext grp : ctx
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
                    .typeImplInit().stmtBlock().stmt(), built));
        }
        built.put(ctx, representationClass);
    }

    @Override public void exitParameterDeclGroup(
            @NotNull Resolve.ParameterDeclGroupContext ctx) {
        for (TerminalNode t : ctx.ID()) {
            built.put(t, new ParameterDef(t.getText()));
        }
    }

    @Override public void exitModule(@NotNull Resolve.ModuleContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitModuleArgument(
            @NotNull Resolve.ModuleArgumentContext ctx) {
        Expr e = (Expr) built.get(ctx.progExp());
        if ( e instanceof VarNameRef ) {
            //Todo: I think it's ok to do getChild(0) here; we know we're
            //dealing with a VarNameRef (so our (2nd) child ctx must be progNamedExp)...
            Resolve.ProgNamedExpContext argAsNamedExp =
                    (Resolve.ProgNamedExpContext) ctx.progExp()
                            .getChild(0).getChild(0);
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

    @Override public void exitStmt(@NotNull Resolve.StmtContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitAssignStmt(
            @NotNull Resolve.AssignStmtContext ctx) {
        built.put(ctx, buildPrimitiveInfixStat("assign", ctx.left, ctx.right));
    }

    @Override public void exitSwapStmt(
            @NotNull Resolve.SwapStmtContext ctx) {
        built.put(ctx, buildPrimitiveInfixStat("swap", ctx.left, ctx.right));
    }

    @Override public void exitCallStmt(
            @NotNull Resolve.CallStmtContext ctx) {
        built.put(ctx, new CallStat((MethodCall) built.get(ctx.progParamExp())));
    }

    @Override public void exitWhileStmt(
            @NotNull Resolve.WhileStmtContext ctx) {
        WhileStat w = new WhileStat((Expr) built.get(ctx.progExp()));
        w.stats.addAll(Utils.collect(Stat.class, ctx.stmt(), built));
        built.put(ctx, w);
    }

    @Override public void exitProgNestedExp(
            @NotNull Resolve.ProgNestedExpContext ctx) {
        built.put(ctx, built.get(ctx.progExp()));
    }

    @Override public void exitProgPrimaryExp(
            @NotNull Resolve.ProgPrimaryExpContext ctx) {
        built.put(ctx, built.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            @NotNull Resolve.ProgPrimaryContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitProgParamExp(
            @NotNull Resolve.ProgParamExpContext ctx) {
        List<Expr> args = Utils.collect(Expr.class, ctx.progExp(), built);
        built.put(ctx, new MethodCall(buildQualifier(ctx.qualifier, ctx.name),
                ctx.name.getText(), args));
    }

    @Override public void exitProgInfixExp(
            @NotNull Resolve.ProgInfixExpContext ctx) {
        Token name = Utils.convertProgramOp(ctx.op).name;
        List<Expr> args = Utils.collect(Expr.class, ctx.progExp(), built);
        built.put(
                ctx,
                new MethodCall(
                        buildQualifier("Std_Integer_Fac", name.getText()), name
                        .getText(), args));
    }

    @Override public void exitProgNamedExp(
            @NotNull Resolve.ProgNamedExpContext ctx) {
        //Todo: this ... isn't right yet..
        built.put(ctx,
                new VarNameRef(new NormalQualifier("this"), ctx.name.getText()));
    }

    @Override public void exitProgMemberExp(
            @NotNull Resolve.ProgMemberExpContext ctx) {
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

    @Override public void exitProgIntegerExp(
            @NotNull Resolve.ProgIntegerExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier("Std_Integer_Fac",
                "Integer_Template"), "Integer", ctx.getText()));
    }

    @Override public void exitProgCharacterExp(
            @NotNull Resolve.ProgCharacterExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier("Std_Character_Fac",
                "Character_Template"), "Character", ctx.getText()));
    }

    @Override public void exitProgStringExp(
            @NotNull Resolve.ProgStringExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier("Std_Char_Str_Fac",
                "Char_Str_Template"), "Char_Str", ctx.getText()));
    }

    @Override public void exitConceptImplModule(
            @NotNull Resolve.ConceptImplModuleContext ctx) {
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
        try {
            ModuleScopeBuilder conceptScope =
                    symtab.getModuleScope(ctx.concept.getText());
            impl.addGetterMethodsAndVarsForConceptualParamsAndGenerics(conceptScope
                    .query(new SymbolTypeQuery<>(Symbol.class)));

            for (OperationSymbol f : moduleScope
                    .query(new SymbolTypeQuery<OperationSymbol>(
                            OperationSymbol.class))) {
                if ( f.isModuleParameter() ) {
                    impl.addOperationParameterModelObjects((FunctionDef) built
                            .get(f.getDefiningTree()));
                }
            }
        }
        catch (NoSuchSymbolException nsse) {
            //Shouldn't happen, if it does, should've yelled about it in semantics
            gen.compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                    ctx.concept, ctx.concept.getText());
        }
        impl.addCtor();
        file.module = impl;
        built.put(ctx, file);
    }

    @Override public void exitFacilityModule(
            @NotNull Resolve.FacilityModuleContext ctx) {
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

    @Override public void exitConceptModule(
            @NotNull Resolve.ConceptModuleContext ctx) {
        ModuleFile file = buildFile();
        SpecModule spec = new SpecModule.ConceptModule(ctx.name.getText(), file);

        if ( ctx.conceptBlock() != null ) {
            spec.types.addAll(Utils.collect(TypeInterfaceDef.class, ctx
                    .conceptBlock().typeModelDecl(), built));
            spec.funcs.addAll(Utils.collect(FunctionDef.class, ctx
                    .conceptBlock().operationDecl(), built));
        }
        spec.addGetterMethodsAndVarsForConceptualParamsAndGenerics(moduleScope
                .query(new SymbolTypeQuery<>(Symbol.class)));

        file.module = spec;
        built.put(ctx, file);
    }

    @Override public void exitEnhancementModule(
            @NotNull Resolve.EnhancementModuleContext ctx) {
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
        spec.addGetterMethodsAndVarsForConceptualParamsAndGenerics(
                moduleScope.getSymbolsOfType(Symbol.class));
        file.module = spec;
        built.put(ctx, file);
    }

    @Override public void exitEnhancementImplModule(
            @NotNull Resolve.EnhancementImplModuleContext ctx) {
        ModuleFile file = buildFile();
        EnhancementImplModule impl =
                new EnhancementImplModule(ctx.name.getText(),
                        ctx.enhancement.getText(), ctx.concept.getText(), file);
        Scope conceptScope = symtab.moduleScopes.get(ctx.concept.getText());
        impl.addDelegateMethods(
                conceptScope.getSymbolsOfType(OperationSymbol.class,
                        GenericSymbol.class, ProgParameterSymbol.class,
                        ProgTypeModelSymbol.class));
        if ( ctx.implBlock() != null ) {
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().operationProcedureDecl(), built));
            impl.funcImpls.addAll(Utils.collect(FunctionImpl.class, ctx
                    .implBlock().procedureDecl(), built));
        }
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
        return t.getChild(0) instanceof Resolve.FacilityModuleContext;
    }

    protected boolean isJavaLocallyAccessibleSymbol(Symbol s)
            throws NoSuchSymbolException {
        //System.out.println("symbol: "+s.getName()+":"+s.getModuleID()+" is locally accessible?");
        boolean result = isJavaLocallyAccessibleSymbol(s.getModuleID());
        //System.out.println(result);
        return result;
    }

    protected boolean isJavaLocallyAccessibleSymbol(String symbolModuleID) {
        //was s defined in the module we're translating?
        if ( moduleScope.getModuleID().equals(symbolModuleID) ) {
            return true;
        }
        else { //was s defined in our parent concept?
            ParseTree thisTree = moduleScope.getDefiningTree();
            if (thisTree instanceof Resolve.ModuleContext) {
                thisTree = thisTree.getChild(0);
            }
            if ( thisTree instanceof Resolve.ConceptImplModuleContext ) {
                Resolve.ConceptImplModuleContext asConceptImpl =
                        (Resolve.ConceptImplModuleContext) thisTree;
                return symbolModuleID.equals(asConceptImpl.concept.getText());
            }
            //Todo: enhancement case.
        }
        return false;
    }

    protected CallStat buildPrimitiveInfixStat(@NotNull String name,
                               @NotNull Resolve.ProgExpContext left,
                               @NotNull Resolve.ProgExpContext right) {
        Qualifier.NormalQualifier qualifier = new NormalQualifier("RESOLVEBase");
        return new CallStat(qualifier, name, (Expr) built.get(left),
                (Expr) built.get(right));
    }

    protected Qualifier buildQualifier(String symQualifier, String symName) {
        try {
            if ( symQualifier == null ) {
                Symbol s =
                        moduleScope.queryForOne(new NameQuery(null,
                                new CommonToken(ResolveLexer.ID, symName), true));
                Qualifier.NormalQualifier q;
                if ( isJavaLocallyAccessibleSymbol(s) ) {
                    //this.<symName>
                    if ( withinFacilityModule() ) {
                        q = new Qualifier.NormalQualifier(moduleScope.getModuleID());
                    }
                    else {
                        q = new Qualifier.NormalQualifier("this");
                    }
                }
                else {
                    //Test_Fac.<symName>
                    q = new Qualifier.NormalQualifier(s.getModuleID());
                }
                return q;
            }
            //We're here: so the call (or thing) was qualified... is the qualifier
            //referring to a facility? Let's check.
            FacilitySymbol s =
                    moduleScope.queryForOne(
                            new NameQuery(null,
                            new CommonToken(ResolveLexer.ID, symQualifier), true))
                            .toFacilitySymbol();

            //Ok, so let's build a facility qualifier from the found 's'.
            return new Qualifier.FacilityQualifier(s);
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            //Todo: symQualifier can be null here -- npe waiting to happen. Address this.
            if ( isJavaLocallyAccessibleSymbol(symQualifier) ) {
                return new Qualifier.NormalQualifier("this");
            }
            return new Qualifier.NormalQualifier(symQualifier);
        }
        catch (UnexpectedSymbolException use) {
            throw new RuntimeException(); //should've been caught looong ago.
        }
    }

    protected Qualifier buildQualifier(Token symQualifier,
                                       @NotNull Token symName) {
        return buildQualifier(symQualifier != null ? symQualifier.getText()
                : null, symName.getText());
    }
}