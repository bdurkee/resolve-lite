package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.semantics.programtype.PTFamily;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.MathSymbolQuery;
import org.resolvelite.semantics.query.UnqualifiedNameQuery;
import org.resolvelite.semantics.symbol.MathInvalidSymbol;
import org.resolvelite.semantics.symbol.MathSymbol;
import org.resolvelite.semantics.symbol.ProgTypeDefinitionSymbol;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.resolvelite.semantics.symbol.Symbol.Quantification;
import org.resolvelite.typereasoning.TypeGraph;

/**
 * Computes types for math and programming expressions and updates stored
 * with this computed type information.
 */
//Todo: Figure out if we want this to build PExps here as well.
public class ComputeTypes extends SetScopes {

    public ParseTreeProperty<MTType> mathTypes = new ParseTreeProperty<>();
    public ParseTreeProperty<MTType> mathTypeValues = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progType = new ParseTreeProperty<>();
    public ParseTreeProperty<PTType> progTypeValues = new ParseTreeProperty<>();

    protected TypeGraph g;
    protected int typeValueDepth = 0;

    ComputeTypes(@NotNull ResolveCompiler rc, @NotNull SymbolTable symtab) {
        super(rc, symtab);
        this.g = symtab.getTypeGraph();
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
            MTType modelType = mathTypeValues.get(ctx.mathTypeExp());
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

        MTType type = mathTypes.get(ctx.mathExp());
        MTType typeValue = mathTypeValues.get(ctx.mathExp());
        if ( typeValue == null ) {
            compiler.errorManager.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
        }
        mathTypes.put(ctx, type);
        mathTypeValues.put(ctx, typeValue);
    }

    private MathSymbol exitMathSymbolExp(Token qualifier, String symbolName,
            ParserRuleContext ctx) {
        MathSymbol intendedEntry = getIntendedEntry(qualifier, symbolName, ctx);

        mathTypes.put(ctx, intendedEntry.getType());
        setSymbolTypeValue(ctx, symbolName, intendedEntry);
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
            throw new RuntimeException(); //This will never fire
        }
        catch (NoSuchSymbolException nsse) {
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                    ctx.getStart(), symbolName);
            return MathInvalidSymbol.getInstance(g, symbolName);
        }
    }

    private void setSymbolTypeValue(ParserRuleContext ctx, String symbolName,
            @NotNull MathSymbol intendedEntry) {
        try {
            if ( intendedEntry.getQuantification() == Quantification.NONE ) {
                mathTypeValues.put(ctx, intendedEntry.getTypeValue());
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
                mathTypeValues.put(ctx, g.MALFORMED);
            }
        }
    }

    protected final void chainMathTypes(ParseTree current, ParseTree child) {
        mathTypes.put(current, mathTypes.get(child));
        mathTypeValues.put(current, mathTypeValues.get(child));
    }

    protected final PExp buildPExp(ParserRuleContext ctx) {
        if ( ctx == null ) return null;
        PExpBuildingListener builder =
                new PExpBuildingListener(mathTypes, mathTypeValues);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
        return builder.getBuiltPExp(ctx);
    }

    protected final String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }
}
