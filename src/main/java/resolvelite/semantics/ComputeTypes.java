package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import resolvelite.compiler.ErrorKind;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.parsing.ResolveParser;
import resolvelite.semantics.symbol.*;

import java.util.*;

public class ComputeTypes extends SetScopes {

    ParseTreeProperty<Type> types = new ParseTreeProperty<>();

    public ComputeTypes(@NotNull ResolveCompiler compiler,
            @NotNull SymbolTable symtab) {
        super(compiler, symtab);
    }

    @Override
    public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        typeVariableGroup(ctx, ctx.Identifier(), ctx.type());
    }

    @Override
    public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        typeVariableGroup(ctx, ctx.Identifier(), ctx.type());
    }

    @Override
    public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        try {
            FunctionSymbol func =
                    (FunctionSymbol) currentScope.resolve(ctx.name.getText());
            Type t = types.get(ctx.type());
            func.setType(t);
            types.put(ctx, t);
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.name, ctx.name.getText());
        }
    }

    @Override
    public void exitType(@NotNull ResolveParser.TypeContext ctx) {
        Type type = null;
        try {
            type = (Type) currentScope.resolve(ctx.name.getText());
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.name, ctx.name.getText());
            type = InvalidType.INSTANCE;
        }
        types.put(ctx, type);
    }

    @Override
    public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        types.put(ctx, types.get(ctx.progPrimary()));
    }

    @Override
    public void exitProgPrimary(@NotNull ResolveParser.ProgPrimaryContext ctx) {
        types.put(ctx, types.get(ctx.getChild(0)));
    }

    @Override
    public void
            exitProgNamedExp(@NotNull ResolveParser.ProgNamedExpContext ctx) {
        try {
            if ( types.get(ctx) != null ) {
                return; //already typed (as is the case for record member refs.
            }
            VariableSymbol varSym =
                    (VariableSymbol) currentScope.resolve(ctx.name.getText());
            Type t = checkForInvalidType(varSym.getType(), null);
            types.put(ctx, t);
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.name, ctx.name.getText());
            types.put(ctx, InvalidType.INSTANCE);
        }
    }

    @Override
    public void
            exitProgParamExp(@NotNull ResolveParser.ProgParamExpContext ctx) {
        try {
            Symbol s = currentScope.resolve(ctx.name.getText());
            if ( s.getClass() != FunctionSymbol.class ) {
                compiler.errorManager.semanticError(
                        ErrorKind.UNEXPECTED_SYMBOL, ctx.name, "a function", s
                                .getClass().getSimpleName());
                types.put(ctx, SymbolTable.VOID);
            }
            FunctionSymbol func = (FunctionSymbol) s;
            types.put(ctx, func.getType());
            /*    int i=0;
                  for (Symbol a : ms.orderedArgs.values() ) { // for each arg
                      CymbolAST argAST = (CymbolAST)args.get(i++);

                      // get argument expression type and expected type
                      Type actualArgType = argAST.evalType;
                      Type formalArgType = ((VariableSymbol)a).type;
                      int targ = actualArgType.getTypeIndex();
                      int tformal = formalArgType.getTypeIndex();

                      // do we need to promote argument type to defined type?
                      argAST.promoteToType = promoteFromTo[targ][tformal];
                      if ( !canAssignTo(actualArgType, formalArgType,
                              argAST.promoteToType) ) {
                          listener.error(text(argAST)+", argument "+
                                  a.name+":<"+a.type+"> of "+ms.name+
                                  "() have incompatible types in "+
                                  text((CymbolAST)id.getParent()));
                      }
                  }*/
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.name, ctx.name.getText());
        }
    }

    @Override
    public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        try {
            ProgTypeDefinitionSymbol intType =
                    (ProgTypeDefinitionSymbol) currentScope.resolve("Integer");
            types.put(ctx, intType);
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.getStart(), "Integer");
            types.put(ctx, InvalidType.INSTANCE);
        }
    }

    @Override
    public void exitVariableMemberExp(
            @NotNull ResolveParser.VariableMemberExpContext ctx) {
        //The first spot had better be a record
        Iterator<TerminalNode> memberIter = ctx.Identifier().iterator();
        ParserRuleContext firstRecordRef = ctx.progNamedExp();

        Type t = types.get(firstRecordRef);
        if ( !(t instanceof RecordReprSymbol) ) {
            compiler.errorManager.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "a record type", "an " + t.getName()
                            + " type ref");
            memberIter.forEachRemaining(s -> types.put(s, InvalidType.INSTANCE));
            types.put(ctx, InvalidType.INSTANCE); //Type the overall thing invalid.
            return;
        }
        RecordReprSymbol currentRecordRef = (RecordReprSymbol) t;

        //now type and make sure each of the members is correct..
        Type curType = null;
        while (memberIter.hasNext()) {
            TerminalNode curTerm = memberIter.next();
            try {
                VariableSymbol s = (VariableSymbol)
                        currentRecordRef.resolve(curTerm.getText());
                curType = s.getType();
                types.put(curTerm, curType);
                if (curType instanceof RecordReprSymbol) {
                    currentRecordRef = (RecordReprSymbol) curType;
                }
            }
            catch (NoSuchSymbolException nsse) {
                symtab.getCompiler().errorManager.semanticError(
                        ErrorKind.NO_SUCH_SYMBOL, curTerm.getSymbol(),
                        curTerm.getSymbol().getText());
                types.put(curTerm, InvalidType.INSTANCE);
                curType = InvalidType.INSTANCE;
            }
        }
        //effectively setting it to the type of the last member access.
        types.put(ctx, curType);
    }

    protected void typeVariableGroup(ParserRuleContext ctx,
            List<TerminalNode> terminalGroup,
            @NotNull ResolveParser.TypeContext typeCtx) {
        Type type = types.get(typeCtx);
        for (TerminalNode t : terminalGroup) {
            try {
                VariableSymbol varSym =
                        (VariableSymbol) currentScope.resolve(t.getText());
                varSym.setType(type);
            }
            catch (NoSuchSymbolException nsse) {
                symtab.getCompiler().errorManager.semanticError(
                        ErrorKind.NO_SUCH_SYMBOL, t.getSymbol(), t.getText());
                types.put(ctx, InvalidType.INSTANCE);
            }
        }
        types.put(ctx, type);
    }

    private Type checkForInvalidType(@Nullable Type t,
            @Nullable ResolveParser.TypeContext typeCtx) {
        if ( t == null ) {
            if ( typeCtx != null ) {
                symtab.getCompiler().errorManager.semanticError(
                        ErrorKind.NO_SUCH_SYMBOL, typeCtx.name,
                        typeCtx.name.getText());
            }
            t = InvalidType.INSTANCE;
        }
        return t;
    }
}
