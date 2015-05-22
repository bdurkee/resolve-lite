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
import org.resolvelite.compiler.tree.ImportCollection.ImportType;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.semantics.programtype.*;
import org.resolvelite.semantics.query.MathSymbolQuery;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;
import java.util.stream.Collectors;

public class DefSymbolsAndScopes extends ResolveBaseListener {

    protected ResolveCompiler compiler;
    protected SymbolTable symtab;
    protected AnnotatedTree tree;
    protected TypeGraph g;

    /**
     * Any quantification-introducing syntactic node (like, e.g., an
     * {@link ResolveParser.MathQuantifiedExpContext}), introduces a level to
     * this stack to reflect the quantification that should be applied to named
     * variables as they are encountered. Note that this may change as the
     * children of the node are processed--for example, MathVarDecs found in the
     * declaration portion of a QuantExp should have quantification
     * (universal or existential) applied, while those found in the body of the
     * QuantExp should have no quantification (unless there is an embedded
     * QuantExp). In this case, QuantExp should not remove its layer, but
     * rather change it to {@code Symbol.Quantification.none}.
     * 
     * This stack is never empty, but rather the bottom layer is always
     * MathSymbolTableEntry.None.
     */
    private Deque<Quantification> activeQuantifications = new LinkedList<>();
    private boolean walkingModuleParameter = false;
    private boolean walkingDefnParameters = false;
    int globalSpecCount = 0;

    DefSymbolsAndScopes(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.activeQuantifications.push(Quantification.NONE);
        this.compiler = rc;
        this.symtab = symtab;
        this.tree = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    @Override public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
        for (ResolveParser.GenericTypeContext generic : ctx.genericType()) {
            try {
                symtab.getInnermostActiveScope().define(
                        new GenericSymbol(g, generic.getText(), generic,
                                getRootModuleID()));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.name, ctx.name.getText());
            }
        }
    }

    @Override public void exitConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterEnhancementModule(
            @NotNull ResolveParser.EnhancementModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void exitEnhancementModule(
            @NotNull ResolveParser.EnhancementModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterConceptImplModule(
            @NotNull ResolveParser.ConceptImplModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void exitConceptImplModule(
            @NotNull ResolveParser.ConceptImplModuleContext ctx) {
        symtab.endScope();
    }

    //just add the named imports already found in ImportListener. Module compile
    //order needs the importlist way before now. So just use it.
    @Override public void enterEnhancementImplModule(
            @NotNull ResolveParser.EnhancementImplModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void exitEnhancementImplModule(
            @NotNull ResolveParser.EnhancementImplModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void exitFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tree.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void exitPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void exitFacilityDecl(
            @NotNull ResolveParser.FacilityDeclContext ctx) {
        try {
            symtab.getInnermostActiveScope().define(
                    new FacilitySymbol(ctx, getRootModuleID(), symtab));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterModuleParameterDecl(
            @NotNull ResolveParser.ModuleParameterDeclContext ctx) {
        walkingModuleParameter = true;
    }

    @Override public void exitModuleParameterDecl(
            @NotNull ResolveParser.ModuleParameterDeclContext ctx) {
        walkingModuleParameter = false;
    }

    @Override public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        MathSymbol exemplar = null;
        MTType modelType = null;
        try {
            //NOTE: Can't walk the whole ctx here just yet. Say exemplar is 'b',
            //and initialization stipulates that b = true. Since we only just get
            //around to adding the (typed) binding for exemplar 'b' below, we
            //won't be able to properly type all of 'ctx's subexpressions right
            //here.
            annotateExpTypesFor(ctx.mathTypeExp(),
                    symtab.getInnermostActiveScope());
            modelType = tree.mathTypeValues.get(ctx.mathTypeExp());
            exemplar =
                    symtab.getInnermostActiveScope()
                            .define(new MathSymbol(symtab.getTypeGraph(),
                                    ctx.exemplar.getText(), modelType, null,
                                    ctx, getRootModuleID())).toMathSymbol();
        }
        catch (DuplicateSymbolException e) {
            throw new RuntimeException("duplicate exemplar!??");
        }
        //now annotate types for all subexpressions within the typeModelDecl
        //tree (e.g. contraints, init, final, etc) before leaving the scope.
        //annotateExpTypesFor(ctx, symtab.getInnermostActiveScope());
        //We aren't doing this.
        symtab.endScope();
        if ( ctx.mathTypeExp().getText().equals(ctx.name.getText()) ) {
            compiler.errorManager.semanticError(ErrorKind.INVALID_MATH_MODEL,
                    ctx.mathTypeExp().getStart(), ctx.mathTypeExp().getText());
        }
        ResolveParser.ConstraintClauseContext constraint =
                ctx.constraintClause() != null ? ctx.constraintClause() : null;
        ResolveParser.EnsuresClauseContext initEnsures =
                ctx.typeModelInit() != null ? ctx.typeModelInit()
                        .ensuresClause() : null;
        try {

            ProgTypeModelSymbol progType =
                    new ProgTypeModelSymbol(symtab.getTypeGraph(),
                            ctx.name.getText(), modelType, new PTFamily(
                                    modelType, ctx.name.getText(),
                                    ctx.exemplar.getText(), constraint,
                                    initEnsures, getRootModuleID()), exemplar,
                            ctx, getRootModuleID());
            symtab.getInnermostActiveScope().define(progType);
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        ProgTypeModelSymbol typeDefn = null;
        try {
            typeDefn =
                    symtab.getInnermostActiveScope()
                            .queryForOne(new NameQuery(null, ctx.name, false))
                            .toProgTypeModelSymbol();
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
        catch (NoSuchSymbolException | UnexpectedSymbolException e) {
            //if we don't find the type model (or we find something
            //that ISN'T a type model) -- it doesn't matter, we'll proceed under
            //the assumption that the thing is a local, standalone type rep.
        }
        String exemplarName =
                typeDefn != null ? typeDefn.getExemplar().getName() : ctx
                        .getText().substring(0, 1).toUpperCase();
        PTType baseType =
                ctx.record() != null ? getProgramType(ctx.record())
                        : getProgramType(ctx.type());

        ResolveParser.EnsuresClauseContext initEnsures =
                ctx.typeImplInit() != null ? ctx.typeImplInit().ensuresClause()
                        : null;

        PTRepresentation reprType =
                new PTRepresentation(g, baseType, ctx.name.getText(), typeDefn,
                        initEnsures, getRootModuleID());
        try {
            symtab.getInnermostActiveScope().define(
                    new ProgVariableSymbol(exemplarName, ctx, reprType,
                            getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {}
        symtab.endScope();
        try {
            symtab.getInnermostActiveScope().define(
                    new ProgReprTypeSymbol(g, ctx.name.getText(), ctx,
                            getRootModuleID(), typeDefn, reprType, ctx
                                    .conventionClause(), ctx
                                    .correspondenceClause()));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterMathTypeTheoremDecl(
            @NotNull ResolveParser.MathTypeTheoremDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void enterMathTypeTheoremUniversalVars(
            @NotNull ResolveParser.MathTypeTheoremUniversalVarsContext ctx) {
        activeQuantifications.push(Quantification.UNIVERSAL);
    }

    @Override public void exitMathTypeTheoremUniversalVars(
            @NotNull ResolveParser.MathTypeTheoremUniversalVarsContext ctx) {
        activeQuantifications.pop();
    }

    @Override public void exitMathTypeTheoremDecl(
            @NotNull ResolveParser.MathTypeTheoremDeclContext ctx) {
        annotateExpTypesFor(ctx, symtab.getInnermostActiveScope());
        annotatePExpsFor(ctx);
        PExp bindingExp = normalizeClause(ctx.bindingExp);
        PExp typeExp = normalizeClause(ctx.typeExp);

        try {
            g.addRelationship(bindingExp, typeExp.getMathTypeValue(),
                    symtab.getInnermostActiveScope());
        }
        catch (IllegalArgumentException iae) {
            iae.printStackTrace();
        }
        symtab.endScope();
    }

    @Override public void enterMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        symtab.startScope(ctx);
    }

    @Override public void enterDefinitionParameterList(
            @NotNull ResolveParser.DefinitionParameterListContext ctx) {
        activeQuantifications.push(Quantification.UNIVERSAL);
    }

    @Override public void exitDefinitionParameterList(
            @NotNull ResolveParser.DefinitionParameterListContext ctx) {
        activeQuantifications.pop();
    }

    @Override public void exitMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        symtab.endScope();

        annotateExpTypesFor(ctx.mathTypeExp(), symtab.getInnermostActiveScope());
        MTType returnType = tree.mathTypeValues.get(ctx.mathTypeExp());
        MTFunction.MTFunctionBuilder builder =
                new MTFunction.MTFunctionBuilder(g, returnType);

        if ( ctx.definitionParameterList() != null ) {
            for (ResolveParser.MathVariableDeclGroupContext grp : ctx
                    .definitionParameterList().mathVariableDeclGroup()) {
                for (TerminalNode t : grp.Identifier()) {
                    try {
                        MTType type =
                                symtab.scopes
                                        .get(ctx)
                                        .queryForOne(
                                                new NameQuery(null,
                                                        t.getText(), true))
                                        .toMathSymbol().getType();
                        builder.paramTypes(type);
                        builder.paramNames(t.getText());
                    }
                    catch (UnexpectedSymbolException | NoSuchSymbolException e) {
                        e.printStackTrace();
                    }
                    catch (DuplicateSymbolException e) {
                        e.printStackTrace();
                    }
                }
            }
            returnType = builder.build();
        }
        MTType typeValue = null;
        if ( ctx.mathAssertionExp() != null ) {
            typeValue = tree.mathTypeValues.get(ctx.mathAssertionExp());
        }

        try {
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, ctx.name.getText(), returnType,
                            typeValue, ctx, getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name.getStart(), ctx.name.getText());
        }
    }

    @Override public void exitConstraintClause(
            @NotNull ResolveParser.ConstraintClauseContext ctx) {
        if ( !(ctx.getParent() instanceof ResolveParser.TypeModelDeclContext) ) {
            insertGlobalAssertion(ctx, ctx.mathAssertionExp());
        }
    }

    @Override public void exitRequiresClause(
            @NotNull ResolveParser.RequiresClauseContext ctx) {
        if ( ctx.getParent().getParent() instanceof ResolveParser.ModuleContext ) {
            insertGlobalAssertion(ctx, ctx.mathAssertionExp());
        }
    }

    private void insertGlobalAssertion(ParserRuleContext ctx,
            ResolveParser.MathAssertionExpContext assertion) {
        String name = ctx.getText() + "_" + globalSpecCount++;
        try {
            symtab.getInnermostActiveScope().define(
                    new GlobalMathAssertionSymbol(name, assertion, ctx,
                            getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.getText());
        }
    }

    @Override public void enterProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        symtab.startScope(ctx);
        try {
            if ( ctx.type() != null ) {
                symtab.getInnermostActiveScope().define(
                        new ProgVariableSymbol(ctx.name.getText(), ctx,
                                getProgramType(ctx.type()), getRootModuleID()));
            }
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void exitProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        symtab.endScope();
        insertFunction(ctx.name, ctx, null, null, ctx.type(), true, null);
    }

    @Override public void enterOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        symtab.startScope(ctx);
        try {
            if ( ctx.type() != null ) {
                symtab.getInnermostActiveScope().define(
                        new ProgVariableSymbol(ctx.name.getText(), ctx,
                                getProgramType(ctx.type()), getRootModuleID()));
            }
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void exitOperationProcedureDecl(
            @NotNull ResolveParser.OperationProcedureDeclContext ctx) {
        symtab.endScope();
        insertFunction(ctx.name, ctx, ctx.requiresClause(),
                ctx.ensuresClause(), ctx.type(), false, null);
    }

    @Override public void enterOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        symtab.startScope(ctx);
        try {
            if ( ctx.type() != null ) {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, ctx.name.getText(), getProgramType(
                                ctx.type()).toMath(), null, ctx,
                                getRootModuleID()));
            }
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {
        symtab.endScope();
        insertFunction(ctx.name, ctx, ctx.requiresClause(),
                ctx.ensuresClause(), ctx.type(), false, null);
    }

    @Override public void exitParameterDeclGroup(
            @NotNull ResolveParser.ParameterDeclGroupContext ctx) {
        PTType programType = getProgramType(ctx.type());
        for (TerminalNode t : ctx.Identifier()) {
            try {
                ProgParameterSymbol.ParameterMode mode =
                        ProgParameterSymbol.getModeMapping().get(
                                ctx.parameterMode().getText());
                symtab.getInnermostActiveScope().define(
                        new ProgParameterSymbol(symtab.getTypeGraph(), t
                                .getText(), mode, programType, ctx,
                                getRootModuleID()));
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    @Override public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        insertVariables(ctx.Identifier(), ctx.type());
    }

    @Override public void exitRecordVariableDeclGroup(
            @NotNull ResolveParser.RecordVariableDeclGroupContext ctx) {
        insertVariables(ctx.Identifier(), ctx.type());
    }

    @Override public void exitMathVariableDeclGroup(
            @NotNull ResolveParser.MathVariableDeclGroupContext ctx) {
        insertMathVariables(ctx, ctx.mathTypeExp(), ctx.Identifier());
    }

    @Override public void exitMathVariableDecl(
            @NotNull ResolveParser.MathVariableDeclContext ctx) {
        insertMathVariables(ctx, ctx.mathTypeExp(), ctx.Identifier());
    }

    /*@Override public void enterMathQuantifiedExp(
            @NotNull ResolveParser.MathQuantifiedExpContext ctx) {
        compiler.info("entering preQuantExp...");
        symtab.startScope(ctx);
        Symbol.Quantification quantification =
                Symbol.Quantification.valueOf(ctx.q.getText());

        activeQuantifications.push(quantification);
    }*/

    private void insertMathVariables(ParserRuleContext ctx,
            ResolveParser.MathTypeExpContext type, TerminalNode... terms) {
        insertMathVariables(ctx, type, Arrays.asList(terms));
    }

    private void insertMathVariables(ParserRuleContext ctx,
            ResolveParser.MathTypeExpContext type, List<TerminalNode> terms) {
        annotateExpTypesFor(type, symtab.getInnermostActiveScope());
        for (TerminalNode t : terms) {
            MTType mathTypeValue = tree.mathTypeValues.get(type);
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, t.getText(), activeQuantifications
                                .peek(), mathTypeValue, null, ctx,
                                getRootModuleID()));
            }
            catch (DuplicateSymbolException e) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    private void insertVariables(List<TerminalNode> terminalGroup,
            ResolveParser.TypeContext type) {
        PTType programType = getProgramType(type);
        for (TerminalNode t : terminalGroup) {
            try {
                ProgVariableSymbol vs =
                        new ProgVariableSymbol(t.getText(), t, programType,
                                getRootModuleID());
                symtab.getInnermostActiveScope().define(vs);
            }
            catch (DuplicateSymbolException dse) {
                compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
    }

    private void insertFunction(@NotNull Token name, ParserRuleContext ctx,
            ResolveParser.RequiresClauseContext requires,
            ResolveParser.EnsuresClauseContext ensures,
            @Nullable ResolveParser.TypeContext type, boolean isProcedure,
            OperationSymbol formalOp) {
        try {
            //TODO: This is dangerous if we aren't using a linked hashmap as our table.
            //There is probably a way to fix this so that the implementation/insertion order
            //for the table doesn't matter.
            List<ProgParameterSymbol> params =
                    symtab.scopes.get(ctx).getSymbolsOfType(
                            ProgParameterSymbol.class);
            Symbol result = null;
            if ( isProcedure ) {
                result =
                        new ProcedureSymbol(name.getText(), ctx,
                                getRootModuleID(), formalOp);
            }
            else {
                result =
                        new OperationSymbol(symtab.getTypeGraph(),
                                name.getText(), ctx, requires, ensures,
                                getProgramType(type), getRootModuleID(),
                                params, walkingModuleParameter);
            }
            symtab.getInnermostActiveScope().define(result);
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, name,
                    name.getText());
        }
    }

    protected PTType getProgramType(@Nullable ResolveParser.TypeContext type) {
        return type == null ? PTVoid.getInstance(g) : getProgramType(type,
                type.qualifier, type.name);
    }

    protected PTType getProgramType(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull Token typeName) {
        return getProgramType(symtab.getInnermostActiveScope(), compiler, ctx,
                qualifier != null ? qualifier.getText() : null,
                typeName.getText());
    }

    /**
     * Returns a {@link PTType} based on context {@code ctx}; {@link PTInvalid}
     * if the symbol retrieved was not typed properly.
     */
    protected static PTType getProgramType(Scope s, ResolveCompiler compiler,
            @NotNull ParserRuleContext ctx, @Nullable String qualifier,
            @NotNull String typeName) {
        try {
            return s.queryForOne(new NameQuery(qualifier, typeName, true))
                    .toProgTypeSymbol().getProgramType();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(),
                    ctx.getStart(), typeName);
        }
        catch (UnexpectedSymbolException use) {
            compiler.errorManager.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "a prog type symbol", typeName,
                    use.getActualSymbolDescription());
        }
        return PTInvalid.getInstance(compiler.symbolTable.getTypeGraph());
    }

    protected PTType getProgramType(@NotNull ResolveParser.RecordContext ctx) {
        Map<String, PTType> fields = new LinkedHashMap<>();
        for (ResolveParser.RecordVariableDeclGroupContext fieldGrp : ctx
                .recordVariableDeclGroup()) {
            PTType grpType = getProgramType(fieldGrp.type());
            for (TerminalNode t : fieldGrp.Identifier()) {
                fields.put(t.getText(), grpType);
            }
        }
        return new PTRecord(g, fields);
    }

    /**
     * Annotates all expressions (and subexpressions) in parsetree {@code ctx}
     * with type info.
     * 
     * @param ctx The subtree to annotate.
     */
    protected final void annotateExpTypesFor(@NotNull ParseTree ctx, Scope s) {
        ComputeTypes annotator = new ComputeTypes(compiler, symtab, tree);
        annotator.setCurrentScope(s);
        ParseTreeWalker.DEFAULT.walk(annotator, ctx);
    }

    protected final void annotatePExpsFor(@NotNull ParseTree ctx) {
        PExpBuildingListener<PExp> builder =
                new PExpBuildingListener<>(symtab.mathPExps,
                        symtab.quantifiedExps, tree);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
    }

    protected PExp normalizeClause(ParserRuleContext ctx) {
        if ( ctx == null ) {
            return g.getTrueExp();
        }
        return symtab.mathPExps.get(ctx);
    }

    protected final String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }
}
