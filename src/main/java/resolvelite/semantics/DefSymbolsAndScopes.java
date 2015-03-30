package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import resolvelite.compiler.ErrorKind;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.compiler.tree.ImportCollection.ImportType;
import resolvelite.compiler.tree.AnnotatedTree;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;
import resolvelite.semantics.symbol.*;

import java.util.List;

/**
 * The first phase of compilation, responsible for defining symbols and building
 * scopes.
 */
public class DefSymbolsAndScopes extends ResolveBaseListener {

    Scope currentScope; // define symbols in this scope
    ResolveCompiler compiler;
    SymbolTable symtab;
    AnnotatedTree tree;

    public DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tree = annotatedTree;
    }

    @Override public void enterPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        currentScope =
                establishModuleScope(ctx.name.getText(), ctx).addImports(
                        tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        currentScope =
                establishModuleScope(ctx.name.getText(), ctx).addImports(
                        tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        currentScope =
                establishModuleScope(ctx.name.getText(), ctx).addImports(
                        tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void enterFacilityDecl(
            @NotNull ResolveParser.FacilityDeclContext ctx) {
        try {
            currentScope.define(new FacilitySymbol(ctx.name.getText(), ctx.spec
                    .getText(), ctx.impl.getText()));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        String name = ctx.name.getText();
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
            AbstractReprSymbol rs = null;
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
                        new VariableSymbol(t.getText(), currentScope);
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

    @Override public void enterOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        insertFunction(ctx.name, ctx);
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
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
                        currentScope));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope();
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        currentScope = currentScope.getEnclosingScope(); // pop scope
    }

    private ModuleScope establishModuleScope(@NotNull String moduleName,
            @NotNull ParserRuleContext ctx) {
        ModuleScope module =
                new ModuleScope(symtab.getGlobalScope(), symtab, moduleName);
        symtab.moduleScopes.put(moduleName, module);
        return module;
    }
}
