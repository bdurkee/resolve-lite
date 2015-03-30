package resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import resolvelite.compiler.ErrorKind;
import resolvelite.compiler.ResolveCompiler;
import resolvelite.misc.Utils;
import resolvelite.parsing.ResolveParser;
import resolvelite.semantics.symbol.*;

import java.util.*;

public class ComputeTypes extends SetScopes {

    ParseTreeProperty<Type> types = new ParseTreeProperty<>();

    public ComputeTypes(@NotNull ResolveCompiler compiler,
            @NotNull SymbolTable symtab) {
        super(compiler, symtab);
    }

    @Override public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        typeVariableDeclGroup(ctx, ctx.Identifier(), ctx.type());
    }

    @Override public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        typeVariableDeclGroup(ctx, ctx.Identifier(), ctx.type());
    }

    @Override public void exitParameterDeclGroup(
            @NotNull ResolveParser.ParameterDeclGroupContext ctx) {
        Type type = types.get(ctx.type());
        for (TerminalNode t : ctx.Identifier()) {
            try {
                ParameterSymbol paramSym =
                        (ParameterSymbol) currentScope.resolve(t.getText());
                paramSym.setType(type);
            }
            catch (NoSuchSymbolException nsse) {
                symtab.getCompiler().errorManager.semanticError(
                        ErrorKind.NO_SUCH_SYMBOL, t.getSymbol(), t.getText());
                types.put(ctx, InvalidType.INSTANCE);
            }
        }
        types.put(ctx, type);
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        try {
            FunctionSymbol func =
                    (FunctionSymbol) currentScope.resolve(ctx.name.getText());
            Type t = types.get(ctx.type());
            if ( t == null ) {
                t =
                        new ProgTypeSymbol("Void", symtab,
                                currentScope.getRootModuleID());
            }
            func.setType(t);
            types.put(ctx, t);
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.name, ctx.name.getText());
        }
    }

    @Override public void exitType(@NotNull ResolveParser.TypeContext ctx) {
        Type type = null;
        Symbol foundSym = null;
        try {
            foundSym = currentScope.resolve(ctx.qualifier, ctx.name);
            type = (Type) foundSym;
        }
        catch (ClassCastException cce) {
            compiler.errorManager.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.name, "a type", foundSym.getClass().getSimpleName());
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.name, ctx.name.getText(),
                    ctx.qualifier, nsse.getMessage());
            type = InvalidType.INSTANCE;
        }
        types.put(ctx, type);
    }

    @Override public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        types.put(ctx, types.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            @NotNull ResolveParser.ProgPrimaryContext ctx) {
        types.put(ctx, types.get(ctx.getChild(0)));
    }

    @Override public void exitProgNamedExp(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {
        try {
            if ( types.get(ctx) != null ) {
                return; //already typed (as is the case for record member refs.
            }
            TypedSymbol sym =
                    (TypedSymbol) currentScope.resolve(ctx.qualifier, ctx.name);
            Type t = checkForInvalidType(sym.getType(), null);
            types.put(ctx, t);
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.name, ctx.name.getText(),
                    ctx.qualifier);
            types.put(ctx, InvalidType.INSTANCE);
        }
    }

    @Override public void exitAssignStmt(
            @NotNull ResolveParser.AssignStmtContext ctx) {
        types.put(ctx, checkTypes(ctx, ctx.left, ctx.right));
    }

    @Override public void exitSwapStmt(
            @NotNull ResolveParser.SwapStmtContext ctx) {
        types.put(ctx, checkTypes(ctx, ctx.left, ctx.right));
    }

    protected Type checkTypes(@NotNull ParserRuleContext parent,
            @NotNull ResolveParser.ProgExpContext t1,
            @NotNull ResolveParser.ProgExpContext t2) {
        Type resultType = null;
        Type progT1 = types.get(t1);
        Type progT2 = types.get(t2);
        if ( !progT1.getName().equals(progT2.getName()) ) {
            compiler.errorManager.semanticError(ErrorKind.INCOMPATIBLE_TYPES,
                    null, t1.getText(), progT1.getName(), t2.getText(), progT2,
                    parent.getText());
            return InvalidType.INSTANCE;
        }
        return progT1;
    }

    @SuppressWarnings("unchecked") @Override public void exitProgParamExp(
            @NotNull ResolveParser.ProgParamExpContext ctx) {
        try {
            Symbol s = currentScope.resolve(ctx.qualifier, ctx.name);
            if ( s.getClass() != FunctionSymbol.class ) {
                compiler.errorManager.semanticError(
                        ErrorKind.UNEXPECTED_SYMBOL, ctx.name, "a function", s
                                .getClass().getSimpleName());
                types.put(ctx, InvalidType.INSTANCE);
            }
            types.put(ctx, checkCallArgs((FunctionSymbol) s, ctx));
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.name, ctx.name.getText());
            types.put(ctx, InvalidType.INSTANCE);
        }
    }

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        types.put(ctx, getProgramType(ctx, "Std_Integer_Fac", "Integer"));
    }

    @Override public void exitProgMemberExp(
            @NotNull ResolveParser.ProgMemberExpContext ctx) {
        //The first spot had better be a record
        Iterator<TerminalNode> memberIter = ctx.Identifier().iterator();
        ParseTree firstRecordRef = ctx.getChild(0);
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
                        currentRecordRef.resolveMember(curTerm.getText());
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

    protected void typeVariableDeclGroup(ParserRuleContext ctx,
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

    /**
     * Returns the symbol representing a basic type such as Integer, Boolean,
     * Character, etc.
     * 
     * @param typeName
     * @return
     */
    private Type getProgramType(@NotNull ParserRuleContext ctx,
            @Nullable String qualifier, @NotNull String typeName) {
        try {
            return (ProgTypeSymbol) currentScope.resolve(qualifier, typeName);
        }
        catch (NoSuchSymbolException nsse) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, ctx.getStart(), typeName);
        }
        return InvalidType.INSTANCE;
    }

    @SuppressWarnings("unchecked") protected Type checkCallArgs(
            @NotNull FunctionSymbol foundSym,
            @NotNull ResolveParser.ProgParamExpContext foundExp) {
        List<? extends Symbol> raw =
                Utils.filter(foundSym.getSymbols(),
                        p -> (p instanceof ParameterSymbol));
        List<ParameterSymbol> formals = (List)raw;
        if (foundExp.progExp().size() != formals.size()) {
            symtab.getCompiler().errorManager.semanticError(
                    ErrorKind.NO_SUCH_SYMBOL, foundExp.name,
                    foundExp.name.getText());
            return InvalidType.INSTANCE;
        }
        int i=0;
        for (ParameterSymbol p : formals) {
            Type actuaArgType = types.get(foundExp.progExp(i++));
            Type formalArgType = p.getType();

            if (!actuaArgType.getName().equals(formalArgType.getName())) {
                symtab.getCompiler().errorManager.semanticError(
                        ErrorKind.NO_SUCH_SYMBOL, foundExp.name,
                        foundExp.name.getText());
                return InvalidType.INSTANCE;
            }
        }
        return foundSym.getType();
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
