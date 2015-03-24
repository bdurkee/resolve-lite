package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import resolvelite.compiler.ErrorKind;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.parsing.ResolveBaseListener;
import resolvelite.parsing.ResolveParser;
import resolvelite.semantics.symbol.*;

import java.util.List;
import java.util.Stack;

/**
 * The first phase of compilation, responsible for defines symbols and building
 * scopes.
 */
public class DefSymbolsAndScopes extends ResolveBaseListener {

    Scope currentScope; // define symbols in this scope
    ResolveCompiler compiler;
    SymbolTable symtab;

    public DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab) {
        this.compiler = rc;
        this.symtab = symtab;
    }

    @Override public void enterPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        currentScope = establishModuleScope(ctx.name.getText(), ctx);
    }

    @Override public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        currentScope = establishModuleScope(ctx.name.getText(), ctx);
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        currentScope = establishModuleScope(ctx.name.getText(), ctx);
    }

    @Override public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        String name = ctx.name.getText();
    }

    @Override public void enterTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        try {
            AbstractReprSymbol rs = null;
            if ( ctx.record() != null ) {
                rs = new RecordReprSymbol(ctx.name.getText(), ctx);
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

    @Override public void enterOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        try {
            FunctionSymbol func = new FunctionSymbol(ctx.name.getText(), ctx);
            symtab.scopes.put(ctx, func);
            currentScope.define(func);
            currentScope = func;
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
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
        ModuleScope module = new ModuleScope(PredefinedScope.INSTANCE);
        symtab.moduleScopes.put(moduleName, module);
        return module;
    }
}
