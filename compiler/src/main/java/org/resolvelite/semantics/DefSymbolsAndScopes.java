package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.ImportCollection.ImportType;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.semantics.symbol.*;

import java.util.List;

/**
 * The first phase of compilation, responsible for defining symbols and building
 * scopes.
 */
public class DefSymbolsAndScopes extends ResolveBaseListener {

    //Todo should symbols representing decls get typed here in this pass?

    Scope currentScope; // define symbols in this scope
    ResolveCompiler compiler;
    SymbolTable symtab;
    AnnotatedTree tree;
    boolean walkingModuleParameters = false;

    public DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tree = annotatedTree;
    }

    @Override public void enterPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        currentScope =
                establishModuleScope(tree, ctx).addImports(
                        tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        currentScope =
                establishModuleScope(tree, ctx).addImports(
                        tree.imports.getImportsOfType(ImportType.NAMED));

        for (ResolveParser.GenericTypeContext generic : ctx.genericType()) {
            try {
                currentScope.define(new GenericSymbol(generic.getText(),
                        currentScope.getRootModuleID()));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.name, ctx.name.getText());
            }
        }
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        currentScope =
                establishModuleScope(tree, ctx).addImports(
                        tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void enterConceptImplModule(
            @NotNull ResolveParser.ConceptImplModuleContext ctx) {
        currentScope =
                establishModuleScope(tree, ctx).addImports(
                        tree.imports.getImportsOfType(ImportType.NAMED))
                        .addImports(ctx.concept.getText());
        //concept impls implicitly import the concepts they impl
    }

    @Override public void enterImplModuleParameterList(
            @NotNull ResolveParser.ImplModuleParameterListContext ctx) {
        walkingModuleParameters = true;
    }

    @Override public void exitImplModuleParameterList(
            @NotNull ResolveParser.ImplModuleParameterListContext ctx) {
        walkingModuleParameters = false;
    }

    @Override public void enterMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        try {
            currentScope.define(new MathSymbol(ctx.name.getText(), );
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterFacilityDecl(
            @NotNull ResolveParser.FacilityDeclContext ctx) {
        try {
            currentScope.define(new FacilitySymbol(ctx.name.getText(), ctx.spec
                    .getText(), ctx.impl.getText(), currentScope
                    .getRootModuleID(), ctx.type(), symtab));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        try {
            currentScope.define(new ProgTypeSymbol(ctx.name.getText(), symtab,
                    currentScope.getRootModuleID()));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        try {
            AbstractReprSymbol rs;
            if ( ctx.record() != null ) {
                rs =
                        new RecordReprSymbol(ctx.name.getText(), ctx, symtab,
                                currentScope.getRootModuleID());
            }
            else {
                throw new UnsupportedOperationException("named repr types not "
                        + "currently supported; only records for now");
                // rs = new NamedReprSymbol(...)
            }
            currentScope.define(rs);
            symtab.scopes.put(ctx, rs); //save the scope
            currentScope = rs; // set cur scope to record type def. scope
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        insertVariables(ctx.Identifier(), ctx.type());
    }

    @Override public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        insertVariables(ctx.Identifier(), ctx.type());
    }

    protected void insertVariables(List<TerminalNode> terminalGroup,
            ResolveParser.TypeContext type) {
        for (TerminalNode t : terminalGroup) {
            try {
                VariableSymbol vs =
                        new VariableSymbol(t.getText(), currentScope,
                                currentScope.getRootModuleID());
                currentScope.define(vs);
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    private void insertFunction(Token name, ParserRuleContext ctx) {
        try {
            FunctionSymbol func =
                    new FunctionSymbol(name.getText(), ctx, symtab,
                            currentScope.getRootModuleID());
            func.isFormalParameter = walkingModuleParameters;
            symtab.scopes.put(ctx, func);
            currentScope.define(func);
            currentScope = func;
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
    }

    @Override public void enterOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        insertFunction(ctx.name, ctx);
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override public void enterOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        insertFunction(ctx.name, ctx);
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override public void enterProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        insertFunction(ctx.name, ctx);
    }

    @Override public void exitProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override public void exitParameterDeclGroup(
            @NotNull ResolveParser.ParameterDeclGroupContext ctx) {
        for (TerminalNode t : ctx.Identifier()) {
            try {
                ParameterSymbol.ParameterMode mode =
                        ParameterSymbol.getModeMapping().get(
                                ctx.parameterMode().getText());
                currentScope.define(new ParameterSymbol(t.getText(), mode,
                        currentScope, currentScope.getRootModuleID()));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope(); // pop scope
    }

    private ModuleScope establishModuleScope(@NotNull AnnotatedTree t,
            @NotNull ParserRuleContext ctx) {
        ModuleScope module =
                new ModuleScope(symtab.getGlobalScope(), symtab, tree);
        symtab.moduleScopes.put(tree.getName(), module);
        return module;
    }
}
