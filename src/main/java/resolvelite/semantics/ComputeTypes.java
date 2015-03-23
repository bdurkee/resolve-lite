package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import resolvelite.compiler.ErrorKind;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.parsing.ResolveParser;
import resolvelite.semantics.symbol.FunctionSymbol;
import resolvelite.semantics.symbol.ProgTypeDefinitionSymbol;
import resolvelite.semantics.symbol.VariableSymbol;

import java.util.List;

public class ComputeTypes extends SetScopes {

    public ComputeTypes(@NotNull ResolveCompiler compiler,
            @NotNull SymbolTable symtab) {
        super(compiler, symtab);
    }

    @Override
    public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        typeVariableGroup(ctx.Identifier(), ctx.type());
    }

    @Override
    public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        typeVariableGroup(ctx.Identifier(), ctx.type());
    }

    @Override
    public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        Type type = null;
        if ( ctx.type() != null ) {
            type = (Type) currentScope.resolve(ctx.type().getText());
            type = checkForInvalidType(type, ctx.type());
        }
        else {
            type = new ProgTypeDefinitionSymbol("Void");
        }
        FunctionSymbol func =
                (FunctionSymbol) currentScope.resolve(ctx.name.getText());
        func.setType(type);
    }

    protected void typeVariableGroup(List<TerminalNode> terminalGroup,
            @NotNull ResolveParser.TypeContext typeCtx) {
        Type type = (Type) currentScope.resolve(typeCtx.name.getText());
        type = checkForInvalidType(type, typeCtx);
        for (TerminalNode t : terminalGroup) {
            VariableSymbol varSym =
                    (VariableSymbol) currentScope.resolve(t.getText());
            if (varSym == null) {
                symtab.getCompiler().errorManager.semanticError(
                        ErrorKind.NO_SUCH_SYMBOL, t.getSymbol(), t.getSymbol()
                                .getText());
                continue;
            }
            varSym.setType(type);
        }
    }

    private Type checkForInvalidType(Type t, ResolveParser.TypeContext typeCtx) {
        if ( t == null ) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, typeCtx.name,
                    typeCtx.name.getText());
            t = InvalidType.INSTANCE;
        }
        return t;
    }
}
