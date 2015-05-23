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
package org.resolvelite.codegen;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.codegen.model.*;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.*;
import org.resolvelite.semantics.programtype.PTGeneric;
import org.resolvelite.semantics.programtype.PTNamed;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.query.SymbolTypeQuery;
import org.resolvelite.semantics.query.UnqualifiedNameQuery;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.codegen.model.Qualifier.NormalQualifier;
import org.resolvelite.codegen.model.Qualifier.FacilityQualifier;

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
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        built.put(ctx, new TypeInterfaceDef(ctx.name.getText()));
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        FunctionDef f = new FunctionDef(ctx.name.getText());
        f.hasReturn = ctx.type() != null;
        f.isStatic = withinFacilityModule();
        for (ResolveParser.ParameterDeclGroupContext grp : ctx
                .operationParameterList().parameterDeclGroup()) {
            for (TerminalNode id : grp.Identifier()) {
                f.params.add((ParameterDef) built.get(id));
            }
        }
        built.put(ctx, f);
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        FunctionImpl f =
                buildFunctionImpl(ctx.name.getText(), ctx.type(), ctx
                        .operationParameterList().parameterDeclGroup(),
                        ctx.variableDeclGroup(), ctx.stmt());
        built.put(ctx, f);
    }

    @Override public void exitProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        FunctionImpl f =
                buildFunctionImpl(ctx.name.getText(), ctx.type(), ctx
                        .operationParameterList().parameterDeclGroup(),
                        ctx.variableDeclGroup(), ctx.stmt());
        f.implementsOper = true;
        built.put(ctx, f);
    }

    protected FunctionImpl buildFunctionImpl(String name,
            ResolveParser.TypeContext type,
            List<ResolveParser.ParameterDeclGroupContext> formalGroupings,
            List<ResolveParser.VariableDeclGroupContext> variableGroupings,
            List<ResolveParser.StmtContext> stats) {
        FunctionImpl f = new FunctionImpl(name);
        f.hasReturn = type != null;
        f.isStatic = withinFacilityModule();
        for (ResolveParser.ParameterDeclGroupContext grp : formalGroupings) {
            f.params.addAll(Utils.collect(ParameterDef.class, grp.Identifier(),
                    built));
        }
        for (ResolveParser.VariableDeclGroupContext grp : variableGroupings) {
            f.vars.addAll(Utils.collect(VariableDef.class, grp.Identifier(),
                    built));
        }
        for (ResolveParser.StmtContext s : stats) {
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
            @NotNull ResolveParser.FacilityDeclContext ctx) {
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
                if ( s instanceof GenericSymbol ) {
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

        for (ResolveParser.EnhancementPairDeclContext pair : ctx
                .enhancementPairDecl()) {
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
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        Expr init = (Expr) built.get(ctx.type());
        for (TerminalNode t : ctx.Identifier()) {
            //System.out.println("adding "+t.getText()+" to built map");
            built.put(t, new VariableDef(t.getSymbol().getText(), init));
        }
    }

    @Override public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        Expr init = (Expr) built.get(ctx.type());
        for (TerminalNode t : ctx.Identifier()) {
            //System.out.println("adding "+t.getText()+" to built map");
            built.put(t, new VariableDef(t.getSymbol().getText(), init));
        }
    }

    @Override public void exitType(@NotNull ResolveParser.TypeContext ctx) {
        built.put(ctx, new TypeInit(buildQualifier(ctx.qualifier, ctx.name),
                ctx.name.getText(), ""));
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        MemberClassDef representationClass =
                new MemberClassDef(ctx.name.getText());
        String exemplarName = "";
        try {
            //Maybe in the future we can assign program types to the ctxs
            ProgReprTypeSymbol x =
                    moduleScope.queryForOne(
                            new UnqualifiedNameQuery(ctx.name.getText()))
                            .toProgReprTypeSymbol();
            exemplarName =
                    ((PTNamed) x.toProgTypeSymbol().getProgramType())
                            .getExemplarName();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            exemplarName = ctx.name.getText().substring(0, 1); //default.
        }
        representationClass.isStatic = withinFacilityModule();
        representationClass.referredToByExemplar = exemplarName;
        if ( ctx.record() != null ) {
            for (ResolveParser.RecordVariableDeclGroupContext grp : ctx
                    .record().recordVariableDeclGroup()) {
                representationClass.fields.addAll(Utils.collect(
                        VariableDef.class, grp.Identifier(), built));
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
            @NotNull ResolveParser.ParameterDeclGroupContext ctx) {
        for (TerminalNode t : ctx.Identifier()) {
            built.put(t, new ParameterDef(t.getText()));
        }
    }

    @Override public void exitModule(@NotNull ResolveParser.ModuleContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitModuleArgument(
            @NotNull ResolveParser.ModuleArgumentContext ctx) {
        Expr e = (Expr) built.get(ctx.progExp());
        if ( e instanceof VarNameRef ) {
            //Todo: I think it's ok to do getChild(0) here; we know we're
            //dealing with a VarNameRef (so our (2nd) child ctx must be progNamedExp)...
            ResolveParser.ProgNamedExpContext argAsNamedExp =
                    (ResolveParser.ProgNamedExpContext) ctx.progExp()
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
            catch (NoSuchSymbolException | DuplicateSymbolException e1) {
                e1.printStackTrace();
            }
        }
        built.put(ctx, e);
    }

    @Override public void exitStmt(@NotNull ResolveParser.StmtContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitAssignStmt(
            @NotNull ResolveParser.AssignStmtContext ctx) {
        built.put(ctx, buildPrimitiveInfixStat("assign", ctx.left, ctx.right));
    }

    @Override public void exitSwapStmt(
            @NotNull ResolveParser.SwapStmtContext ctx) {
        built.put(ctx, buildPrimitiveInfixStat("swap", ctx.left, ctx.right));
    }

    @Override public void exitCallStmt(
            @NotNull ResolveParser.CallStmtContext ctx) {
        built.put(ctx, new CallStat((MethodCall) built.get(ctx.progParamExp())));
    }

    @Override public void exitWhileStmt(
            @NotNull ResolveParser.WhileStmtContext ctx) {
        WhileStat w = new WhileStat((Expr) built.get(ctx.progExp()));
        w.stats.addAll(Utils.collect(Stat.class, ctx.stmt(), built));
        built.put(ctx, w);
    }

    @Override public void exitProgNestedExp(
            @NotNull ResolveParser.ProgNestedExpContext ctx) {
        built.put(ctx, built.get(ctx.progExp()));
    }

    @Override public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        built.put(ctx, built.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            @NotNull ResolveParser.ProgPrimaryContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitProgParamExp(
            @NotNull ResolveParser.ProgParamExpContext ctx) {
        List<Expr> args = Utils.collect(Expr.class, ctx.progExp(), built);
        built.put(ctx, new MethodCall(buildQualifier(ctx.qualifier, ctx.name),
                ctx.name.getText(), args));
    }

    @Override public void exitProgApplicationExp(
            @NotNull ResolveParser.ProgApplicationExpContext ctx) {
        String name = Utils.getNameFromProgramOp(ctx.op.getText());
        List<Expr> args = Utils.collect(Expr.class, ctx.progExp(), built);
        built.put(ctx, new MethodCall(buildQualifier("Std_Integer_Fac", name),
                name, args));

    }

    @Override public void exitProgNamedExp(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {
        //Todo: this ... isn't right yet..
        built.put(ctx,
                new VarNameRef(new NormalQualifier("this"), ctx.name.getText()));
    }

    @Override public void exitProgMemberExp(
            @NotNull ResolveParser.ProgMemberExpContext ctx) {
        List<MemberRef> refs = ctx.Identifier()
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
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        built.put(ctx, new TypeInit(new FacilityQualifier("Std_Integer_Fac",
                "Integer_Template"), "Integer", ctx.getText()));
    }

    @Override public void exitConceptImplModule(
            @NotNull ResolveParser.ConceptImplModuleContext ctx) {
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
            gen.compiler.errorManager.semanticError(ErrorKind.NO_SUCH_MODULE,
                    ctx.concept, ctx.concept.getText());
        }
        impl.addCtor();
        file.module = impl;
        built.put(ctx, file);
    }

    @Override public void exitFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
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
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        ModuleFile file = buildFile();
        SpecModule spec = new SpecModule.Concept(ctx.name.getText(), file);

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

    protected ModuleFile buildFile() {
        AnnotatedTree annotatedTree = gen.getModule();
        return new ModuleFile(annotatedTree, Utils.groomFileName(annotatedTree
                .getFileName()));
    }

    protected boolean withinFacilityModule() {
        ParseTree t = gen.getModule().getRoot();
        return t.getChild(0) instanceof ResolveParser.FacilityModuleContext;
    }

    protected boolean isLocallyAccessibleSymbol(Symbol s)
            throws NoSuchSymbolException {
        ModuleScopeBuilder module = symtab.getModuleScope(s.getModuleID());

        return (moduleScope.getModuleID().equals(s.getModuleID()) || gen
                .getModule().getRoot().getChild(0) instanceof ResolveParser.ConceptModuleContext);
    }

    protected CallStat buildPrimitiveInfixStat(@NotNull String name,
            @NotNull ResolveParser.ProgExpContext left,
            @NotNull ResolveParser.ProgExpContext right) {
        NormalQualifier qualifier = new NormalQualifier("ResolveBase");
        return new CallStat(qualifier, name, (Expr) built.get(left),
                (Expr) built.get(right));
    }

    protected Qualifier buildQualifier(String symQualifier, String symName) {
        try {
            //The user has chosen not to qualify their symbol, so at this point
            //symName might refer to something local, or something accessible
            //from an imported module...
            //NOTE: In the language's present state, it CANNOT be referring to
            //to something brought in via a facilitydecl--they would've had to
            //explicitly qualify if this were the case.
            if ( symQualifier == null ) {
                Symbol s =
                        moduleScope.queryForOne(new NameQuery(null, symName,
                                true));
                NormalQualifier q;
                if ( isLocallyAccessibleSymbol(s) ) {
                    //this.<symName>
                    if ( withinFacilityModule() ) {
                        q = new NormalQualifier(moduleScope.getModuleID());
                    }
                    else {
                        q = new NormalQualifier("this");
                    }
                }
                else {
                    //Test_Fac.<symName>
                    q = new NormalQualifier(s.getModuleID());
                }
                return q;
            }
            //We're here: so the call was qualified... is the qualifier
            //referring to a facility? Let's check.
            Symbol s =
                    moduleScope.queryForOne(new NameQuery(null, symQualifier,
                            true));
            //Looks like it is!, let's see if what we found is actually a facility
            if ( !(s instanceof FacilitySymbol) ) {
                throw new RuntimeException("non-facility qualifier... "
                        + "what's going on here?");
            }
            //Ok, so let's build a facility qualifier from the found 's'.
            return new FacilityQualifier((FacilitySymbol) s);
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            //Todo: symQualifier can be null here -- npe waiting to happen. Address this.
            return new NormalQualifier(symQualifier);
        }
    }

    protected Qualifier buildQualifier(Token symQualifier,
            @NotNull Token symName) {
        return buildQualifier(symQualifier != null ? symQualifier.getText()
                : null, symName.getText());
    }
}