package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.semantics.programtype.PTFamily;
import org.resolvelite.semantics.programtype.PTInvalid;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.programtype.PTVoid;
import org.resolvelite.semantics.query.MathSymbolQuery;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.query.UnqualifiedNameQuery;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.resolvelite.semantics.symbol.Symbol.Quantification;
import org.resolvelite.typereasoning.TypeGraph;

public class ComputeTypes extends SetScopes {

    protected TypeGraph g;
    protected AnnotatedTree tree;
    protected int typeValueDepth = 0;

    ComputeTypes(@NotNull ResolveCompiler rc, SymbolTable symtab,
            AnnotatedTree t) {
        super(rc, symtab);
        this.g = symtab.getTypeGraph();
        this.tree = t;
    }

    @Override public void exitParameterDeclGroup(
            @NotNull ResolveParser.ParameterDeclGroupContext ctx) {

        for (TerminalNode t : ctx.Identifier()) {
            try {
                ProgParameterSymbol param =
                        currentScope.queryForOne(
                                new UnqualifiedNameQuery(t.getText()))
                                .toProgParameterSymbol();
                param.setProgramType(tree.progTypeValues.get(ctx.progType()));
            }
            catch (NoSuchSymbolException | DuplicateSymbolException e) {
                compiler.errorManager.semanticError(e.getErrorKind(),
                        t.getSymbol(), t.getText());
            }
        }
    }

    @Override public void exitProgType(
            @NotNull ResolveParser.ProgTypeContext ctx) {
        PTType progType = PTInvalid.getInstance(g);
        MTType mathType = g.INVALID;
        try {
            ProgTypeSymbol type =
                    currentScope.queryForOne(
                            new NameQuery(ctx.qualifier, ctx.name, true))
                            .toProgTypeSymbol();
            tree.mathTypes.put(ctx, g.SSET);
            progType = type.getProgramType();
            mathType = type.getModelType();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(), ctx.name,
                    ctx.name.getText());
        }
        tree.progTypeValues.put(ctx, progType);
        tree.mathTypeValues.put(ctx, mathType);
    }

    @Override public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        try {
            ProgTypeDefinitionSymbol t =
                    currentScope.queryForOne(
                            new UnqualifiedNameQuery(ctx.name.getText(),
                                    ImportStrategy.IMPORT_NONE,
                                    FacilityStrategy.FACILITY_IGNORE, true,
                                    true)).toProgTypeDefinitionSymbol();
            PExp constraint =
                    ctx.constraintClause() != null ? buildPExp(ctx
                            .constraintClause()) : null;
            PExp initRequires =
                    ctx.typeModelInit() != null ? buildPExp(ctx.typeModelInit()
                            .requiresClause()) : null;
            PExp initEnsures =
                    ctx.typeModelInit() != null ? buildPExp(ctx.typeModelInit()
                            .ensuresClause()) : null;
            PExp finalRequires =
                    ctx.typeModelFinal() != null ? buildPExp(ctx
                            .typeModelFinal().requiresClause()) : null;
            PExp finalEnsures =
                    ctx.typeModelFinal() != null ? buildPExp(ctx
                            .typeModelFinal().ensuresClause()) : null;
            MTType modelType = tree.mathTypeValues.get(ctx.mathTypeExp());
            PTType familyType =
                    new PTFamily(modelType, ctx.name.getText(),
                            ctx.exemplar.getText(), constraint, initRequires,
                            initEnsures, finalRequires, finalEnsures);
            t.setProgramType(familyType);
            t.setModelType(modelType);
            t.getExemplar().setTypes(modelType, null);
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            e.printStackTrace();//shouldnt happen
        }
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        typeFunctionLikeThing(ctx.name, ctx.progType());
    }

    private void typeFunctionLikeThing(@NotNull Token name,
            ResolveParser.ProgTypeContext type) {
        try {
            OperationSymbol op =
                    currentScope.queryForOne(new NameQuery(null, name, true))
                            .toOperationSymbol();
            PTType returnType;
            if ( type == null ) {
                returnType = PTVoid.getInstance(g);
            }
            else {
                returnType = tree.progTypeValues.get(type);
            }
            op.setReturnType(returnType);
        }
        catch (NoSuchSymbolException | DuplicateSymbolException ex) {
            compiler.errorManager.semanticError(ex.getErrorKind(), name,
                    name.getText());
        }
    }

    @Override public void exitRequiresClause(
            @NotNull ResolveParser.RequiresClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitEnsuresClause(
            @NotNull ResolveParser.EnsuresClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitMathAssertionExp(
            @NotNull ResolveParser.MathAssertionExpContext ctx) {
        chainMathTypes(ctx, ctx.getChild(0));
    }

    @Override public void exitMathPrimeExp(
            @NotNull ResolveParser.MathPrimeExpContext ctx) {
        chainMathTypes(ctx, ctx.mathPrimaryExp());
    }

    @Override public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        chainMathTypes(ctx, ctx.getChild(0));
    }

    @Override public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        exitMathSymbolExp(null, ctx.getText(), ctx);
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        exitMathSymbolExp(null, ctx.getText(), ctx);
    }

    @Override public void enterMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        typeValueDepth++;
    }

    @Override public void exitMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        typeValueDepth--;

        MTType type = tree.mathTypes.get(ctx.mathExp());
        MTType typeValue = tree.mathTypeValues.get(ctx.mathExp());
        if ( typeValue == null ) {
            compiler.errorManager.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
            typeValue = g.INVALID; // not a type? let's give it an invalid value then
        }
        tree.mathTypes.put(ctx, type);
        tree.mathTypeValues.put(ctx, typeValue);
    }

    private MathSymbol exitMathSymbolExp(Token qualifier, String symbolName,
            ParserRuleContext ctx) {
        MathSymbol intendedEntry = getIntendedEntry(qualifier, symbolName, ctx);
        if ( intendedEntry == null ) {
            tree.mathTypes.put(ctx, g.INVALID);
        }
        else {
            tree.mathTypes.put(ctx, intendedEntry.getType());
            setSymbolTypeValue(ctx, symbolName, intendedEntry);
        }
        return intendedEntry;
    }

    private MathSymbol getIntendedEntry(Token qualifier, String symbolName,
            ParserRuleContext ctx) {
        try {
            return currentScope.queryForOne(
                    new MathSymbolQuery(qualifier, symbolName, ctx.getStart()))
                    .toMathSymbol();
        }
        catch (DuplicateSymbolException dse) {
            throw new RuntimeException();
        }
        catch (NoSuchSymbolException nsse) {
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                    ctx.getStart(), symbolName);
            return null;
        }
    }

    private void setSymbolTypeValue(ParserRuleContext ctx, String symbolName,
            @NotNull MathSymbol intendedEntry) {
        try {
            if ( intendedEntry.getQuantification() == Quantification.NONE ) {
                tree.mathTypeValues.put(ctx, intendedEntry.getTypeValue());
            }
            else {
                if ( intendedEntry.getType().isKnownToContainOnlyMTypes() ) {
                    //mathTypeValues.put(ctx, new MTNamed(g, symbolName));
                }
            }
        }
        catch (SymbolNotOfKindTypeException snokte) {
            if ( typeValueDepth > 0 ) {
                //I had better identify a type
                compiler.errorManager
                        .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                ctx.getStart(), symbolName);
                tree.mathTypeValues.put(ctx, g.INVALID);
            }
        }
    }

    protected final void chainMathTypes(ParseTree current, ParseTree child) {
        tree.mathTypes.put(current, tree.mathTypes.get(child));
        tree.mathTypeValues.put(current, tree.mathTypeValues.get(child));
    }

    protected final PExp buildPExp(ParserRuleContext ctx) {
        if ( ctx == null ) return null;
        PExpBuildingListener builder =
                new PExpBuildingListener(tree.mathTypes, tree.mathTypeValues);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
        return builder.getBuiltPExp(ctx);
    }

    protected final String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }
}
