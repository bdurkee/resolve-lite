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
import org.resolvelite.semantics.ModuleScope;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.ParameterSymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModelBuilder extends ResolveBaseListener {
    public ParseTreeProperty<OutputModelObject> built =
            new ParseTreeProperty<>();
    @NotNull private final ModuleScope moduleScope;
    @NotNull private final CodeGenerator gen;
    @NotNull private final SymbolTable symtab;

    public ModelBuilder(@NotNull CodeGenerator g, @NotNull SymbolTable symtab) {
        this.gen = g;
        this.moduleScope = symtab.moduleScopes.get(g.getModule().getName());
        this.symtab = symtab;
    }

    @Override public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        built.put(ctx, new TypeDef(ctx.name.getText()));
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
        FunctionImpl f = new FunctionImpl(ctx.name.getText());
        f.hasReturn = ctx.type() != null;
        f.isStatic = withinFacilityModule();
        for (ResolveParser.ParameterDeclGroupContext grp : ctx
                .operationParameterList().parameterDeclGroup()) {
            f.params.addAll(collectModelsFor(ParameterDef.class,
                    grp.Identifier(), built));
        }
        for (ResolveParser.VariableDeclGroupContext grp : ctx
                .variableDeclGroup()) {
            f.vars.addAll(collectModelsFor(VariableDef.class, grp.Identifier(),
                    built));
        }
        built.put(ctx, f);
    }

    @Override public void exitProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        FunctionImpl f = new FunctionImpl(ctx.name.getText());
        f.hasReturn = ctx.type() != null;
        f.isStatic = withinFacilityModule();
        f.implementsOper = true;
        for (ResolveParser.ParameterDeclGroupContext grp : ctx
                .operationParameterList().parameterDeclGroup()) {
            f.params.addAll(collectModelsFor(ParameterDef.class,
                    grp.Identifier(), built));
        }
        built.put(ctx, f);
    }

    @Override public void exitFacilityDecl(
            @NotNull ResolveParser.FacilityDeclContext ctx) {
        FacilityDef f = new FacilityDef(ctx.name.getText(), ctx.spec.getText());
        f.isStatic = withinFacilityModule();
        List<LayeredFacilityInstantiation> layers = new ArrayList<>();

        LayeredFacilityInstantiation basePtr =
                new LayeredFacilityInstantiation(ctx.spec.getText(),
                        ctx.impl.getText());
        basePtr.isProxied = false;

        List<Expr> generics = collectModelsFor(Expr.class, ctx.type(), built);
        List<Expr> specArgs =
                ctx.specArgs == null ? new ArrayList<>() : collectModelsFor(
                        Expr.class, ctx.specArgs.moduleArgument(), built);

        basePtr.args.addAll(generics);
        basePtr.args.addAll(specArgs);

        for (ResolveParser.EnhancementPairDeclContext pair : ctx
                .enhancementPairDecl()) {
            LayeredFacilityInstantiation layer =
                    new LayeredFacilityInstantiation(pair.spec.getText(),
                            pair.impl.getText());
            //layer.args.addAll(coreArgs);
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
        try {
            if ( ctx.qualifier == null ) {
                Symbol s = moduleScope.resolve(null, ctx.name.getText(), true);
                String qual =
                        isLocallyAccessibleSymbol(s) ? "this" : s
                                .getRootModuleID();
                built.put(ctx, new MethodCall(qual, "get" + ctx.name.getText()));
                return;
            }
            Symbol s = moduleScope.resolve(null, ctx.qualifier, true);
            if ( !(s instanceof FacilitySymbol) ) {
                throw new RuntimeException("non-facility qualifier... "
                        + "whats going on here?");
            }
            FacilityDefinedTypeInit init =
                    new FacilityDefinedTypeInit(
                            new FacilityDefinedTypeInit.FacilityQualifier(
                                    (FacilitySymbol) s), ctx.name.getText(), "");
            built.put(ctx, init);
        }
        catch (NoSuchSymbolException nsse) {
            //couldn't find a facility for s? Then the qualifier must identify
            //a module, thus, we're dealing with a user defined type that
            //doesn't come through a facility
            built.put(ctx, new MethodCall(ctx.qualifier.getText(), "get"
                    + ctx.name.getText()));
        }
    }

    public boolean isLocallyAccessibleSymbol(Symbol s)
            throws NoSuchSymbolException {
        ModuleScope module = symtab.getModuleScope(s.getRootModuleID());
        return module.getWrappedModuleTree().getRoot().getChild(0) instanceof ResolveParser.ConceptModuleContext;
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        MemberClassDef representationClass =
                new MemberClassDef(ctx.name.getText());
        representationClass.isStatic = withinFacilityModule();
        if ( ctx.record() != null ) {
            for (ResolveParser.RecordVariableDeclGroupContext grp : ctx
                    .record().recordVariableDeclGroup()) {
                representationClass.fields.addAll(collectModelsFor(
                        VariableDef.class, grp.Identifier(), built));
            }
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

    @Override public void exitProgNamedExp(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {

    }

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        FacilityDefinedTypeInit init =
                new FacilityDefinedTypeInit(
                        new FacilityDefinedTypeInit.FacilityQualifier(
                                "Std_Integer_Fac", "Integer_Template"),
                        "Integer", ctx.getText());
        built.put(ctx, init);
    }

    @Override public void exitConceptImplModule(
            @NotNull ResolveParser.ConceptImplModuleContext ctx) {
        ModuleFile file = buildFile();
        ConceptImplModule impl =
                new ConceptImplModule(ctx.name.getText(), ctx.concept.getText(), file);

        if ( ctx.implBlock() != null ) {
            impl.funcImpls.addAll(collectModelsFor(FunctionImpl.class, ctx
                    .implBlock().procedureDecl(), built));
            impl.funcImpls.addAll(collectModelsFor(FunctionImpl.class, ctx
                    .implBlock().operationProcedureDecl(), built));
            impl.repClasses.addAll(collectModelsFor(MemberClassDef.class, ctx
                    .implBlock().typeRepresentationDecl(), built));
            impl.facilityVars.addAll(collectModelsFor(FacilityDef.class, ctx
                    .implBlock().facilityDecl(), built));
        }
        try {
            ModuleScope conceptScope =
                    symtab.getModuleScope(ctx.concept.getText());
            for (ParameterSymbol s : conceptScope.getFormalParameters()) {
                impl.funcImpls.add(new FunctionImpl(s));
            }
        }
        catch (NoSuchSymbolException nsse) {
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
        FacilityImplModule impl = new FacilityImplModule(ctx.name.getText(), file);

        if ( ctx.facilityBlock() != null ) {
            impl.facilities.addAll(collectModelsFor(FacilityDef.class, ctx
                    .facilityBlock().facilityDecl(), built));
            impl.funcImpls.addAll(collectModelsFor(FunctionImpl.class, ctx
                    .facilityBlock().operationProcedureDecl(), built));
            impl.repClasses.addAll(collectModelsFor(MemberClassDef.class, ctx
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
            spec.types.addAll(collectModelsFor(TypeDef.class, ctx
                    .conceptBlock().typeModelDecl(), built));
            spec.funcs.addAll(collectModelsFor(FunctionDef.class, ctx
                    .conceptBlock().operationDecl(), built));
        }
        for (ResolveParser.GenericTypeContext generic : ctx.genericType()) {
            spec.funcs.add(new FunctionDef(generic));
        }
        for (ParameterSymbol s : moduleScope.getFormalParameters()) {
            spec.funcs.add(new FunctionDef(s));
        }
        file.module = spec;
        built.put(ctx, file);
    }

    protected ModuleFile buildFile() {
        AnnotatedTree annotatedTree = gen.getModule();
        return new ModuleFile(annotatedTree, Utils.groomFileName(annotatedTree
                .getFileName()));
    }

    protected static <T extends OutputModelObject> List<T> collectModelsFor(
            Class<T> expectedModelType, List<? extends ParseTree> nodes,
            ParseTreeProperty<OutputModelObject> annotations) {
        return nodes.stream().map(x -> expectedModelType
                .cast(annotations.get(x))).collect(Collectors.toList());
    }

    public boolean withinFacilityModule() {
        ParseTree t = gen.getModule().getRoot();
        return t.getChild(0) instanceof ResolveParser.FacilityModuleContext;
    }

}