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
package resolvelite.codegen;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import resolvelite.codegen.model.*;
import resolvelite.compiler.tree.AnnotatedTree;
import resolvelite.compiler.tree.ImportCollection;
import resolvelite.misc.Utils;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;
import resolvelite.semantics.ModuleScope;
import resolvelite.semantics.NoSuchSymbolException;
import resolvelite.semantics.SymbolTable;

import java.util.*;

public class ModelBuilder extends ResolveBaseListener {

    public ParseTreeProperty<OutputModelObject> built =
            new ParseTreeProperty<>();

    protected final TypeRefInitFactory INTEGER_INIT_FACTORY =
            new IntegerTypeInitFactory();

    protected final TypeRefInitFactory BOOLEAN_INIT_FACTORY =
            new IntegerTypeInitFactory();

    @NotNull private final ModuleScope moduleScope;
    @NotNull private final CodeGenerator gen;

    public ModelBuilder(@NotNull CodeGenerator g,
            @NotNull SymbolTable scopeRepository) {
        this.gen = g;
        this.moduleScope =
                scopeRepository.moduleScopes.get(g.getModule().getName());
    }

    @Override public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        built.put(ctx, new TypeDecl(ctx.name.getText()));
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        FunctionDecl f = new FunctionDecl(ctx.name.getText());
        f.hasReturn = ctx.type() != null;
        f.isStatic = withinFacilityModule();
        for (ResolveParser.ParameterDeclGroupContext grp : ctx
                .operationParameterList().parameterDeclGroup()) {
            for (TerminalNode id : grp.Identifier()) {
                f.params.add((ParameterDecl) built.get(id));
            }
        }
        built.put(ctx, f);
    }

    @Override public void exitFacilityDecl(
            @NotNull ResolveParser.FacilityDeclContext ctx) {
        FacilityInstanceDecl f =
                new FacilityInstanceDecl(ctx.name.getText(), ctx.spec.getText());
        f.isStatic = withinFacilityModule();
        List<LayeredFacilityInstantiation> layers = new ArrayList<>();

        LayeredFacilityInstantiation basePtr =
                new LayeredFacilityInstantiation(ctx.spec.getText(),
                        ctx.impl.getText());
        basePtr.isProxied = false;

        List<Argument> specArgs =
                ctx.specArgs == null ? new ArrayList<>() : collectModelsFor(
                        Argument.class, ctx.specArgs.moduleArgument(), built);

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

    @Override public void exitParameterDeclGroup(
            @NotNull ResolveParser.ParameterDeclGroupContext ctx) {
        for (TerminalNode t : ctx.Identifier()) {
            built.put(t, new ParameterDecl(t.getText()));
        }
    }

    @Override public void exitModule(@NotNull ResolveParser.ModuleContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        AnnotatedTree annotatedTree = gen.getModule();
        ModuleFile file =
                new ModuleFile(annotatedTree, Utils.groomFileName(annotatedTree
                        .getFileName()));
        file.targetDir =
                ImportRef.listifyFileString(annotatedTree.getFileName());
        FacilityImpl impl = new FacilityImpl(ctx.name.getText(), file);

        if ( ctx.facilityBlock() != null ) {
            impl.facilities.addAll(collectModelsFor(FacilityInstanceDecl.class,
                    ctx.facilityBlock().facilityDecl(), built));
        }
        file.module = impl;
        file.imports = buildImports(annotatedTree);
        built.put(ctx, file);
    }

    @Override public void exitModuleArgument(
            @NotNull ResolveParser.ModuleArgumentContext ctx) {
        built.put(ctx, new Argument((Expr) built.get(ctx.progExp())));
    }

    @Override public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        built.put(ctx, built.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            @NotNull ResolveParser.ProgPrimaryContext ctx) {
        built.put(ctx, built.get(ctx.getChild(0)));
    }

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        built.put(ctx, INTEGER_INIT_FACTORY.buildTypeInit(ctx.getText()));
    }

    @Override public void exitConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        AnnotatedTree annotatedTree = gen.getModule();
        ModuleFile file =
                new ModuleFile(annotatedTree, Utils.groomFileName(annotatedTree
                        .getFileName()));
        file.targetDir =
                ImportRef.listifyFileString(annotatedTree.getFileName());
        SpecModule spec = new SpecModule.Concept(ctx.name.getText(), file);

        if ( ctx.conceptBlock() != null ) {
            spec.types.addAll(collectModelsFor(TypeDecl.class, ctx
                    .conceptBlock().typeModelDecl(), built));
            spec.funcs.addAll(collectModelsFor(FunctionDecl.class, ctx
                    .conceptBlock().operationDecl(), built));
        }
        /*
         * for (ModuleParameterAST ast : e.getParameters()) {
         * FunctionDecl paramFunc =
         * new FunctionDecl("get" + ast.getName().getText());
         * paramFunc.hasReturn = true;
         * specModule.funcs.add(paramFunc);
         * }
         */
        file.module = spec;
        file.imports = buildImports(annotatedTree);
        built.put(ctx, file);
    }

    /**
     * Converts the imports in an {@link ImportCollection} to a set of
     * {@link ImportRef}s.
     * 
     * @param m A module builder maintaining a collection of imports
     * @return a set of model-friendly imports.
     */
    protected Set<ImportRef> buildImports(@NotNull AnnotatedTree m) {
        Set<ImportRef> result = new HashSet<ImportRef>();

        for (String s : m.imports
                .getImportsExcluding(ImportCollection.ImportType.EXTERNAL)) {
            try {
                AnnotatedTree e = gen.getCompiler() //
                        .symbolTable.getModuleScope(s) //
                                .getWrappedModuleTree();
                result.add(new ImportRef(e.getFileName()));
            }
            catch (NoSuchSymbolException nsse) {} //shouldn't happen.
        }
        //Todo: handle external imports
        return result;
    }

    protected static <T extends OutputModelObject> List<T> collectModelsFor(
            Class<T> expectedModelType, List<? extends ParseTree> nodes,
            ParseTreeProperty<OutputModelObject> annotations) {
        List<T> result = new ArrayList<T>();
        for (ParseTree x : nodes) {
            result.add(expectedModelType.cast(annotations.get(x)));
        }
        return result;
    }

    public boolean withinFacilityModule() {
        ParseTree t = gen.getModule().getRoot();
        return t instanceof ResolveParser.FacilityModuleContext;
    }

    public static class IntegerTypeInitFactory implements TypeRefInitFactory {

        @Override public InitCall buildTypeInit(String initialValue) {
            return new InitCall(new InitCall.Qualifier("Std_Integer_Fac",
                    "Integer_Template"), "Integer", initialValue);
        }
    }

    public static class booleanTypeInitFactory implements TypeRefInitFactory {

        @Override public InitCall buildTypeInit(String initialValue) {
            return new InitCall(new InitCall.Qualifier("Std_Boolean_Fac",
                    "Boolean_Template"), "Boolean", initialValue);
        }
    }
}