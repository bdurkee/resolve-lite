package org.resolvelite.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.semantics.programtype.*;
import org.resolvelite.semantics.query.MathSymbolQuery;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.query.ProgVariableQuery;
import org.resolvelite.semantics.query.UnqualifiedNameQuery;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.resolvelite.semantics.symbol.Symbol.Quantification;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;

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
                param.setProgramType(tree.progTypeValues.get(ctx.type()));
            }
            catch (NoSuchSymbolException | DuplicateSymbolException e) {
                compiler.errorManager.semanticError(e.getErrorKind(),
                        t.getSymbol(), t.getText());
            }
        }
    }

    @Override public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        typeVariableDeclGroup(ctx, ctx.Identifier(), ctx.type());
    }

    @Override public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        typeVariableDeclGroup(ctx, ctx.Identifier(), ctx.type());
    }

    @Override public void exitType(@NotNull ResolveParser.TypeContext ctx) {
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

    @Override public void exitRecord(@NotNull ResolveParser.RecordContext ctx) {
        Map<String, PTType> fields = new LinkedHashMap<>();
        for (ResolveParser.RecordVariableDeclGroupContext fieldGrp : ctx
                .recordVariableDeclGroup()) {
            for (TerminalNode t : fieldGrp.Identifier()) {
                fields.put(t.getText(), tree.progTypeValues.get(t));
            }
        }
        PTRecord record = new PTRecord(g, fields);
        tree.progTypeValues.put(ctx, record);
        tree.mathTypes.put(ctx, g.SSET);
        tree.mathTypeValues.put(ctx, record.toMath());
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

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        try {
            ProgReprTypeSymbol repr =
                    currentScope.queryForOne(
                            new NameQuery(null, ctx.name, true))
                            .toProgReprTypeSymbol();
            PTType baseType =
                    ctx.record() != null ? tree.progTypeValues
                            .get(ctx.record()) : tree.progTypeValues.get(ctx
                            .type());
            PTType wrappingRepresentation =
                    new PTRepresentation(g, baseType, ctx.name.getText(),
                            repr.getDefinition());
            repr.setRepresentationType(wrappingRepresentation);
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(), ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        super.enterOperationDecl(ctx);
        typeFunctionLikeThing(ctx, ctx.name, ctx.type());
    }

    @Override public void enterOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        super.enterOperationProcedureDecl(ctx);
        typeFunctionLikeThing(ctx, ctx.name, ctx.type());
    }

    @Override public void exitProgNestedExp(
            @NotNull ResolveParser.ProgNestedExpContext ctx) {
        tree.progTypes.put(ctx, tree.progTypes.get(ctx.progExp()));
        tree.mathTypes.put(ctx, tree.mathTypes.get(ctx.progExp()));
    }

    @Override public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        tree.progTypes.put(ctx, tree.progTypes.get(ctx.progPrimary()));
        tree.mathTypes.put(ctx, tree.mathTypes.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            @NotNull ResolveParser.ProgPrimaryContext ctx) {
        tree.progTypes.put(ctx, tree.progTypes.get(ctx.getChild(0)));
        tree.mathTypes.put(ctx, tree.mathTypes.get(ctx.getChild(0)));
    }

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        tree.progTypes.put(ctx,
                getProgramType(ctx, "Std_Integer_Fac", "Integer"));
        tree.mathTypes.put(ctx, g.Z);
    }

    @Override public void exitProgMemberExp(
            @NotNull ResolveParser.ProgMemberExpContext ctx) {
        ParseTree firstRecordRef = ctx.getChild(0);
        PTType first = tree.progTypes.get(firstRecordRef);

        //first we sanity check the first to ensure we're dealing with a record
        if ( !first.isAggregateType() ) {
            compiler.errorManager.semanticError(
                    ErrorKind.ILLEGAL_MEMBER_ACCESS, ctx.getStart(),
                    ctx.getText(), ctx.getChild(0).getText());
            tree.progTypes.put(ctx, PTInvalid.getInstance(g));
            tree.mathTypes.put(ctx, g.INVALID);
            return;
        }
        PTRepresentation curAggregateType = (PTRepresentation) first;

        //note this will represent the rightmost field type when finished.
        PTType curFieldType = curAggregateType;

        //now make sure our mem accesses aren't nonsense.
        for (TerminalNode term : ctx.Identifier()) {
            PTRecord recordType = (PTRecord) curAggregateType.getBaseType();
            curFieldType = recordType.getFieldType(term.getText());

            if ( curFieldType == null ) {
                compiler.errorManager.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                        term.getSymbol(), term.getText());
                curFieldType = PTInvalid.getInstance(g);
                break;
            }
            if ( curFieldType.isAggregateType() ) {
                curAggregateType = (PTRepresentation) curFieldType;
            }
        }
        tree.progTypes.put(ctx, curFieldType);
        tree.mathTypes.put(ctx, curFieldType.toMath());
    }

    @Override public void exitProgNamedExp(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {
        try {
            ProgVariableSymbol variable =
                    currentScope
                            .queryForOne(
                                    new ProgVariableQuery(ctx.qualifier,
                                            ctx.name, true))
                            .toProgVariableSymbol();
            tree.progTypes.put(ctx, variable.getProgramType());
            exitMathSymbolExp(ctx, ctx.qualifier, ctx.name.getText());
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(), ctx.name,
                    ctx.name.getText());
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
        exitMathSymbolExp(ctx, null, ctx.getText());
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        exitMathSymbolExp(ctx, null, ctx.getText());
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

    private MathSymbol exitMathSymbolExp(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull String symbolName) {
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

    private void typeFunctionLikeThing(@NotNull ParserRuleContext ctx,
            @NotNull Token name, ResolveParser.TypeContext type) {
        try {
            OperationSymbol op =
                    currentScope.queryForOne(new NameQuery(null, name, true))
                            .toOperationSymbol();
            PTType returnType;
            if ( type == null ) {
                returnType = PTVoid.getInstance(g);
            }
            else {
                returnType = getProgramType(ctx, type.qualifier, type.name);
            }
            op.setReturnType(returnType);
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(), name,
                    name.getText());
        }
    }

    protected void typeVariableDeclGroup(@NotNull ParserRuleContext ctx,
            @NotNull List<TerminalNode> terminalGroup,
            @NotNull ResolveParser.TypeContext typeCtx) {
        MTType mathTypeValue = tree.mathTypeValues.get(typeCtx);
        PTType progTypeValue = tree.progTypeValues.get(typeCtx);
        for (TerminalNode t : terminalGroup) {
            try {
                ProgVariableSymbol variable =
                        currentScope.queryForOne(new ProgVariableQuery(null, t
                                .getSymbol().getText()));
                variable.setProgramType(progTypeValue);
                tree.progTypeValues.put(t, progTypeValue);
                tree.mathTypeValues.put(t, mathTypeValue);
            }
            catch (NoSuchSymbolException | DuplicateSymbolException e) {
                compiler.errorManager.semanticError(e.getErrorKind(),
                        t.getSymbol(), t.getSymbol().getText());
            }
        }
        //guess we can set it for the overall group too for laughs.
        tree.progTypeValues.put(ctx, progTypeValue);
        tree.mathTypeValues.put(ctx, mathTypeValue);
    }

    protected PTType getProgramType(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull Token typeName) {
        return getProgramType(ctx, qualifier != null ? qualifier.getText()
                : null, typeName.getText());
    }

    /**
     * For returning symbols representing a basic type such as Integer,
     * Boolean, Character, etc
     */
    protected PTType getProgramType(@NotNull ParserRuleContext ctx,
            @Nullable String qualifier, @NotNull String typeName) {
        ProgTypeSymbol result = null;
        try {
            return currentScope
                    .queryForOne(new NameQuery(qualifier, typeName, true))
                    .toProgTypeSymbol().getProgramType();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(),
                    ctx.getStart(), typeName);
        }
        return PTInvalid.getInstance(g);
    }

    protected final void chainMathTypes(ParseTree current, ParseTree child) {
        tree.mathTypes.put(current, tree.mathTypes.get(child));
        tree.mathTypeValues.put(current, tree.mathTypeValues.get(child));
    }

    protected final void chainProgramTypes(ParseTree current, ParseTree child) {
        tree.progTypes.put(current, tree.progTypes.get(child));
        tree.progTypeValues.put(current, tree.progTypeValues.get(child));
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
