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
import org.resolvelite.compiler.tree.ImportCollection;
import org.resolvelite.compiler.tree.ImportCollection.ImportType;
import org.resolvelite.compiler.tree.ResolveToken;
import org.resolvelite.misc.Utils;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PExpBuildingListener;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.programtype.*;
import org.resolvelite.semantics.query.*;
import org.resolvelite.semantics.symbol.*;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;
import java.util.stream.Collectors;

public class DefSymbolsAndScopes2 extends ResolveBaseListener {

    private static final TypeComparison<PSymbol, MTFunction> EXACT_DOMAIN_MATCH =
            new ExactDomainMatch();
    private static final Comparator<MTType> EXACT_PARAMETER_MATCH =
            new ExactParameterMatch();

    private final TypeComparison<PSymbol, MTFunction> INEXACT_DOMAIN_MATCH =
            new InexactDomainMatch();
    private final TypeComparison<PExp, MTType> INEXACT_PARAMETER_MATCH =
            new InexactParameterMatch();

    protected ResolveCompiler compiler;
    protected SymbolTable symtab;
    protected AnnotatedTree tr;
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
     * rather change it to {@code Quantification.NONE}.
     * 
     * This stack is never empty, but rather the bottom layer is always
     * {@code Quantification.NONE}.
     */
    private Deque<Quantification> activeQuantifications = new LinkedList<>();

    private MathSymbol exemplarSymbol = null;
    private ProgTypeModelSymbol curTypeDefnSymbol = null;

    private PTRepresentation reprType = null;

    protected int typeValueDepth = 0;
    protected boolean walkingMathDot = false;
    protected MTType currentSeg = null;

    public DefSymbolsAndScopes2(@NotNull ResolveCompiler rc,
            @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.activeQuantifications.push(Quantification.NONE);
        this.compiler = rc;
        this.symtab = symtab;
        this.tr = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    @Override public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tr.imports.getImportsOfType(ImportType.NAMED));

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

    @Override public void enterConceptImplModule(
            @NotNull ResolveParser.ConceptImplModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText())
                .addImports(tr.imports.getImportsOfType(ImportType.NAMED))
                .addRelatedModules(ctx.concept.getText());
    }

    @Override public void exitConceptImplModule(
            @NotNull ResolveParser.ConceptImplModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterEnhancementModule(
            @NotNull ResolveParser.EnhancementModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tr.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void exitEnhancementModule(
            @NotNull ResolveParser.EnhancementModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterEnhancementImplModule(
            @NotNull ResolveParser.EnhancementImplModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText())
                .addImports(tr.imports.getImportsOfType(ImportType.NAMED))
                .addRelatedModules(ctx.enhancement.getText());
    }

    @Override public void exitEnhancementImplModule(
            @NotNull ResolveParser.EnhancementImplModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tr.imports.getImportsOfType(ImportType.NAMED));
    }

    @Override public void exitPrecisModule(
            @NotNull ResolveParser.PrecisModuleContext ctx) {
        symtab.endScope();
    }

    @Override public void enterTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        symtab.startScope(ctx);
    }

    //Think of this as parse node derived alias to "midTypeModelDecl"
    @Override public void exitMathTypeModelExp(
            @NotNull ResolveParser.MathTypeModelExpContext ctx) {
        try {
            ResolveParser.TypeModelDeclContext curTypeModel =
                    ((ResolveParser.TypeModelDeclContext) ctx.getParent());

            exemplarSymbol =
                    symtab.getInnermostActiveScope().addBinding(
                            curTypeModel.exemplar.getText(), curTypeModel,
                            tr.mathTypeValues.get(ctx.mathTypeExp()));
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), ctx.getText());
        }
    }

    @Override public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        symtab.endScope();
        try {
            PExp constraint =
                    getPExpFor(ctx.constraintClause() != null ? ctx
                            .constraintClause() : null);
            PExp initEnsures =
                    getPExpFor(ctx.typeModelInit() != null ? ctx
                            .typeModelInit().ensuresClause() : null);

            MTType modelType =
                    tr.mathTypeValues.get(ctx.mathTypeModelExp().mathTypeExp());

            ProgTypeModelSymbol progType =
                    new ProgTypeModelSymbol(symtab.getTypeGraph(),
                            ctx.name.getText(), modelType, new PTFamily(
                                    modelType, ctx.name.getText(),
                                    ctx.exemplar.getText(), constraint,
                                    initEnsures, getRootModuleID()),
                            exemplarSymbol, ctx, getRootModuleID());
            symtab.getInnermostActiveScope().define(progType);
            exemplarSymbol = null;
        }
        catch (DuplicateSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL, ctx.name,
                    ctx.name.getText());
        }
    }

    @Override public void enterTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        symtab.startScope(ctx);
        try {
            curTypeDefnSymbol =
                    symtab.getInnermostActiveScope()
                            .queryForOne(
                                    new NameQuery(null, ctx.name.getText(),
                                            false)).toProgTypeModelSymbol();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(), ctx.name,
                    ctx.name.getText());
        }
    }

    //again, think of this as a standin to "midTypeRepresentationDecl"
    @Override public void exitTypeRepresentationType(
            @NotNull ResolveParser.TypeRepresentationTypeContext ctx) {
        PTType t = ctx.type() != null ? tr.progTypeValues.get(ctx.type()) :
                tr.progTypeValues.get(ctx.record());
        ResolveParser.TypeRepresentationDeclContext typeRep =
                ((ResolveParser.TypeRepresentationDeclContext)ctx.getParent());

        reprType = new PTRepresentation(g, t, typeRep.name.getText(),
                curTypeDefnSymbol, getRootModuleID());

        String exemplarName = "";
        if ( curTypeDefnSymbol != null ) {
            exemplarName = curTypeDefnSymbol.getProgramType().getExemplarName();
        }
        else {
            exemplarName = typeRep.name.getText();
        }
        try {
            symtab.getInnermostActiveScope().define(
                    new ProgVariableSymbol(exemplarName, ctx, reprType,
                            getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            e.printStackTrace();
        }
    }

    @Override public void exitTypeRepresentationDecl(
            @NotNull ResolveParser.TypeRepresentationDeclContext ctx) {
        symtab.endScope();

        ProgReprTypeSymbol repr =
                new ProgReprTypeSymbol(g, ctx.name.getText(), ctx,
                        getRootModuleID(), curTypeDefnSymbol, reprType,
                        ctx.conventionClause(), ctx.correspondenceClause());
        curTypeDefnSymbol = null;
        reprType = null;
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
        PExp bindingExp = getPExpFor(ctx.bindingExp);
        PExp typeExp = getPExpFor(ctx.typeExp);
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

        MTType returnType = tr.mathTypeValues.get(ctx.mathTypeExp());
        MTFunction.MTFunctionBuilder builder =
                new MTFunction.MTFunctionBuilder(g, returnType);

        if ( ctx.definitionParameterList() != null ) {
            for (ResolveParser.MathVariableDeclGroupContext grp : ctx
                    .definitionParameterList().mathVariableDeclGroup()) {
                MTType type = tr.mathTypeValues.get(grp.mathTypeExp());
                for (TerminalNode t : grp.Identifier()) {
                    builder.paramTypes(type);
                    builder.paramNames(t.getText());
                }
            }
            returnType = builder.build();
        }
        MTType typeValue = null;
        if ( ctx.mathAssertionExp() != null ) {
            typeValue = tr.mathTypeValues.get(ctx.mathAssertionExp());
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

    //-----------------------------------------------
    // P R O G    E X P    T Y P I N G
    //-----------------------------------------------

    @Override public void exitProgNestedExp(
            @NotNull ResolveParser.ProgNestedExpContext ctx) {
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progExp()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progExp()));
    }

    @Override public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.progPrimary()));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.progPrimary()));
    }

    @Override public void exitProgPrimary(
            @NotNull ResolveParser.ProgPrimaryContext ctx) {
        tr.progTypes.put(ctx, tr.progTypes.get(ctx.getChild(0)));
        tr.mathTypes.put(ctx, tr.mathTypes.get(ctx.getChild(0)));
    }

    @Override public void exitProgMemberExp(
            @NotNull ResolveParser.ProgMemberExpContext ctx) {
        ParseTree firstRecordRef = ctx.getChild(0);
        PTType first = tr.progTypes.get(firstRecordRef);

        //start by checking the first to ensure we're dealing with a record
        if ( !first.isAggregateType() ) {
            compiler.errorManager.semanticError(
                    ErrorKind.ILLEGAL_MEMBER_ACCESS, ctx.getStart(),
                    ctx.getText(), ctx.getChild(0).getText());
            tr.progTypes.put(ctx, PTInvalid.getInstance(g));
            tr.mathTypes.put(ctx, g.INVALID);
            return;
        }
        PTRepresentation curAggregateType = (PTRepresentation) first;

        //note this will represent the rightmost field type when finished.
        PTType curFieldType = curAggregateType;

        //now we need to make sure our mem accesses aren't nonsense.
        for (TerminalNode term : ctx.Identifier()) {
            PTRecord recordType = (PTRecord) curAggregateType.getBaseType();
            curFieldType = recordType.getFieldType(term.getText());
            if ( curFieldType == null ) {
                compiler.errorManager.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                        term.getSymbol(), term.getText());
                curFieldType = PTInvalid.getInstance(g);
                tr.progTypes.put(term, curFieldType);
                tr.mathTypes.put(term, curFieldType.toMath());
                break;
            }
            tr.progTypes.put(term, curFieldType);
            tr.mathTypes.put(term, curFieldType.toMath());

            if ( curFieldType.isAggregateType() ) {
                curAggregateType = (PTRepresentation) curFieldType;
            }
        }
        tr.progTypes.put(ctx, curFieldType);
        tr.mathTypes.put(ctx, curFieldType.toMath());
    }

    @Override public void exitProgNamedExp(
            @NotNull ResolveParser.ProgNamedExpContext ctx) {
        try {
            ProgVariableSymbol variable =
                    symtab.getInnermostActiveScope()
                            .queryForOne(
                                    new ProgVariableQuery(ctx.qualifier,
                                            ctx.name, true));
            tr.progTypes.put(ctx, variable.getProgramType());
            exitMathSymbolExp(ctx, ctx.qualifier, ctx.name.getText());
            return;
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errorManager.semanticError(e.getErrorKind(), ctx.name,
                    ctx.name.getText());
        }
        catch (UnexpectedSymbolException use) {
            compiler.errorManager.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.name, "a variable reference", ctx.name.getText(),
                    use.getActualSymbolDescription());
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
    }

    @Override public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        PTType progType =
                getProgramType(symtab.getInnermostActiveScope(), compiler, ctx,
                        "Std_Integer_Fac", "Integer");
        tr.progTypes.put(ctx, progType);
        tr.mathTypes.put(ctx, g.Z);
    }

    @Override public void exitProgParamExp(
            @NotNull ResolveParser.ProgParamExpContext ctx) {
        typeOperationSym(ctx, ctx.qualifier, ctx.name, ctx.progExp());
    }

    @Override public void exitProgApplicationExp(
            @NotNull ResolveParser.ProgApplicationExpContext ctx) {
        typeOperationSym(ctx, new ResolveToken("Std_Integer_Fac"), ctx.op,
                ctx.progExp());
    }

    protected void typeOperationSym(ParserRuleContext ctx,
                                    Token qualifier, Token name,
                                    List<ResolveParser.ProgExpContext> args) {
        List<PTType> argTypes = args.stream().map(tr.progTypes::get)
                .collect(Collectors.toList());
        Token opAsName = Utils.getNameFromProgramOp(name.getText());
        try {
            OperationSymbol opSym = symtab.getInnermostActiveScope().queryForOne(
                    new OperationQuery(qualifier, opAsName, argTypes,
                            SymbolTable.FacilityStrategy.FACILITY_INSTANTIATE,
                            SymbolTable.ImportStrategy.IMPORT_NAMED));
            tr.progTypes.put(ctx, opSym.getReturnType());
            tr.mathTypes.put(ctx, opSym.getReturnType().toMath());
            return;
        }
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            List<String> argStrList = args.stream()
                    .map(ResolveParser.ProgExpContext::getText)
                    .collect(Collectors.toList());
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_OPERATION,
                    ctx.getStart(), name, argStrList, argTypes);
        }
        tr.progTypes.put(ctx, PTInvalid.getInstance(g));
        tr.mathTypes.put(ctx, MTInvalid.getInstance(g));
    }

    //-----------------------------------------------
    // M A T H    E X P    T Y P I N G
    //-----------------------------------------------

    @Override public void enterMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        typeValueDepth++;
    }

    @Override public void exitMathTypeExp(
            @NotNull ResolveParser.MathTypeExpContext ctx) {
        typeValueDepth--;
        MTType type = tr.mathTypes.get(ctx.mathExp());
        MTType typeValue = tr.mathTypeValues.get(ctx.mathExp());
        if ( typeValue == null ) {
            compiler.errorManager.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
            typeValue = g.INVALID;
        }
        tr.mathTypes.put(ctx, type);
        tr.mathTypeValues.put(ctx, typeValue);
    }

    @Override public void exitConstraintClause(
            @NotNull ResolveParser.ConstraintClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitConventionClause(
            @NotNull ResolveParser.ConventionClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitCorrespondenceClause(
            @NotNull ResolveParser.CorrespondenceClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitRequiresClause(
            @NotNull ResolveParser.RequiresClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitEnsuresClause(
            @NotNull ResolveParser.EnsuresClauseContext ctx) {
        chainMathTypes(ctx, ctx.mathAssertionExp());
    }

    @Override public void exitMathNestedExp(
            @NotNull ResolveParser.MathNestedExpContext ctx) {
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

    @Override public void exitMathIntegerExp(
            @NotNull ResolveParser.MathIntegerExpContext ctx) {
        tr.mathTypes.put(ctx, g.Z);
    }

    @Override public void exitMathInfixExp(
            @NotNull ResolveParser.MathInfixExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, ctx.op, ctx.mathExp());
    }

    @Override public void exitMathOutfixExp(
            @NotNull ResolveParser.MathOutfixExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, new ResolveToken(ctx.lop.getText()
                + "..." + ctx.rop.getText()), ctx.mathExp());
    }

    @Override public void exitMathFunctionExp(
            @NotNull ResolveParser.MathFunctionExpContext ctx) {
        typeMathFunctionLikeThing(ctx, null, ctx.name, ctx.mathExp());
    }

    @Override public void enterMathCrossTypeExp(
            @NotNull ResolveParser.MathCrossTypeExpContext ctx) {
        typeValueDepth++;
    }

    @Override public void exitMathCrossTypeExp(
            @NotNull ResolveParser.MathCrossTypeExpContext ctx) {
        List<MTCartesian.Element> fieldTypes = new ArrayList<>();

        for (ResolveParser.MathVariableDeclGroupContext grp : ctx
                .mathVariableDeclGroup()) {
            MTType grpType = tr.mathTypeValues.get(grp.mathTypeExp());
            for (TerminalNode t : grp.Identifier()) {
                fieldTypes.add(new MTCartesian.Element(t.getText(), grpType));
            }
        }
        tr.mathTypes.put(ctx, g.MTYPE);
        tr.mathTypeValues.put(ctx, new MTCartesian(g, fieldTypes));
        typeValueDepth--;
    }

    @Override public void exitMathSetCollectionExp(
            @NotNull ResolveParser.MathSetCollectionExpContext ctx) {
        tr.mathTypes.put(ctx, g.SSET);
        if (ctx.mathExp().isEmpty()) {
            tr.mathTypeValues.put(ctx, g.EMPTY_SET);
        }
        else {
            List<MTType> powersets = ctx.mathExp().stream()
                    .map(e -> new MTPowersetApplication(g, tr.mathTypes.get(e)))
                    .collect(Collectors.toList());
            MTUnion u = new MTUnion(g, powersets);
            tr.mathTypeValues.put(ctx, u);
        }
        if (typeValueDepth > 0) {

            // construct a union chain and see if all the component types
            // are known to contain only sets.
            List<MTType> elementTypes = ctx.mathExp().stream()
                    .map(tr.mathTypes::get).collect(Collectors.toList());

            MTUnion chainedTypes = new MTUnion(g, elementTypes);

            if (!chainedTypes.isKnownToContainOnlyMTypes() ||
                    ctx.mathExp().isEmpty()) {
                compiler.errorManager
                        .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                ctx.getStart(), ctx.getText());
                tr.mathTypeValues.put(ctx, g.INVALID);
                return;
            }
            tr.mathTypeValues.put(ctx, chainedTypes);
        }
    }

    @Override public void exitMathSetBuilderExp(
            @NotNull ResolveParser.MathSetBuilderExpContext ctx) {
        tr.mathTypes.put(ctx, g.SSET);
        MTType singleParamType =
                tr.mathTypes.get(ctx.mathVariableDecl().mathTypeExp());
        MTType typeValue =
                new MTFunction.MTFunctionBuilder(g, g.POWERSET).paramTypes(
                        singleParamType).build();
        tr.mathTypeValues.put(ctx, typeValue);
    }

    @Override public void exitMathEntailsAddendum(
            @NotNull ResolveParser.MathEntailsAddendumContext ctx) {

        List<Symbol> symbolsInThisScope = symtab.getInnermostActiveScope()
                .query(new SymbolTypeQuery<Symbol>(Symbol.class));
        List<MathSymbol> coerceableMathSyms = new ArrayList<>();
        for (Symbol s : symbolsInThisScope) {
            try {
                coerceableMathSyms.add(s.toMathSymbol());
            } catch (UnexpectedSymbolException use) {}
        }
        List<String> renames = ctx.Identifier().stream()
                .map(ParseTree::getText).collect(Collectors.toList());
        for (ResolveParser.MathDotExpContext e : ctx.mathDotExp()) {
            int last = e.mathFunctionApplicationExp().size() - 1;
            renames.add(e.mathFunctionApplicationExp().get(last).getText());
        }
        MTType newType = tr.mathTypeValues.get(ctx.mathTypeExp());
        for (MathSymbol retypeSym : coerceableMathSyms) {
            if (renames.contains(retypeSym.getName())
                    && retypeSym.getModuleID().equals(tr.getName())) {
                retypeSym.setMathType(newType);
            }
        }
    }

    @Override public void enterMathDotExp(
            @NotNull ResolveParser.MathDotExpContext ctx) {
        walkingMathDot = true;
    }

    @Override public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        exitMathSymbolExp(ctx, null, ctx.name.getText());
    }

    private MathSymbol getIntendedEntry(Token qualifier, String symbolName,
            ParserRuleContext ctx) throws NoSuchSymbolException {
        try {
            return symtab
                    .getInnermostActiveScope()
                    .queryForOne(
                            new MathSymbolQuery(qualifier, symbolName, ctx
                                    .getStart())).toMathSymbol();
        }
        catch (DuplicateSymbolException dse) {
            compiler.errorManager.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), symbolName);
        }
        return null;
    }

    private void setSymbolTypeValue(ParserRuleContext ctx, String symbolName,
            @NotNull MathSymbol intendedEntry) {
        try {
            if ( intendedEntry.getQuantification() == Quantification.NONE ) {
                tr.mathTypeValues.put(ctx, intendedEntry.getTypeValue());
            }
            else {
                if ( intendedEntry.getType().isKnownToContainOnlyMTypes() ) {
                    tr.mathTypeValues.put(ctx, new MTNamed(g, symbolName));
                }
            }
        }
        catch (SymbolNotOfKindTypeException snokte) {
            if ( typeValueDepth > 0 ) {
                //I had better identify a type
                compiler.errorManager
                        .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                ctx.getStart(), symbolName);
                tr.mathTypeValues.put(ctx, g.INVALID);
            }
        }
    }

    private MathSymbol exitMathSymbolExp(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull String symbolName) {
        MathSymbol intendedEntry = null;
        try {
            intendedEntry = getIntendedEntry(qualifier, symbolName, ctx);
        }
        catch (NoSuchSymbolException e) {
            if ( walkingMathDot && currentSeg != null
                    && currentSeg instanceof MTCartesian ) {
                tr.mathTypes.put(ctx, currentSeg);
                return null; //just cut out.
            }
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_SYMBOL,
                    ctx.getStart(), symbolName);
        }
        if ( intendedEntry == null ) {
            tr.mathTypes.put(ctx, g.INVALID);
        }
        else {
            tr.mathTypes.put(ctx, intendedEntry.getType());
            setSymbolTypeValue(ctx, symbolName, intendedEntry);
        }
        if ( walkingMathDot ) currentSeg = tr.mathTypes.get(ctx);
        return intendedEntry;
    }

    private void typeMathFunctionLikeThing(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull Token name,
            ResolveParser.MathExpContext... args) {
        typeMathFunctionLikeThing(ctx, qualifier, name, Arrays.asList(args));
    }

    private void typeMathFunctionLikeThing(@NotNull ParserRuleContext ctx,
            @Nullable Token qualifier, @NotNull Token name,
            List<ResolveParser.MathExpContext> args) {
        MTFunction foundExpType;
        foundExpType =
                PSymbol.getConservativePreApplicationType(g, args, tr.mathTypes);

        compiler.info("expression: " + ctx.getText() + "("
                + ctx.getStart().getLine() + ","
                + ctx.getStop().getCharPositionInLine() + ") of type "
                + foundExpType.toString());
        MathSymbol intendedEntry =
                getIntendedFunction(ctx, qualifier, name, args);

        if ( intendedEntry == null ) {
            tr.mathTypes.put(ctx, g.INVALID);
            return;
        }
        MTFunction expectedType = (MTFunction) intendedEntry.getType();

        //We know we match expectedType--otherwise the above would have thrown
        //an exception.
        tr.mathTypes.put(ctx, expectedType.getRange());
        if ( typeValueDepth > 0 || name.getText().equals("Powerset")
                || name.getText().equals("union") ) {
            //  if ( typeValueDepth > 0 ) {

            //I had better identify a type
            MTFunction entryType = (MTFunction) intendedEntry.getType();

            List<MTType> arguments = new ArrayList<>();
            MTType argTypeValue;
            for (ParserRuleContext arg : args) {
                argTypeValue = tr.mathTypeValues.get(arg);
                if ( argTypeValue == null ) {
                    compiler.errorManager.semanticError(
                            ErrorKind.INVALID_MATH_TYPE, arg.getStart(),
                            arg.getText());
                }
                arguments.add(argTypeValue);
            }
            MTType applicationType =
                    entryType.getApplicationType(intendedEntry.getName(),
                            arguments);
            tr.mathTypeValues.put(ctx, applicationType);
        }
    }

    private MathSymbol getIntendedFunction(@NotNull ParserRuleContext ctx,
                                           @Nullable Token qualifier, @NotNull Token name,
                                           @NotNull List<ResolveParser.MathExpContext> args) {
        tr.mathTypes.put(ctx, PSymbol.getConservativePreApplicationType(g,
                args, tr.mathTypes));
        PSymbol e = (PSymbol)getPExpFor(ctx);
        MTFunction eType = (MTFunction)e.getMathType();
        String operatorStr = name.getText();

        List<MathSymbol> sameNameFunctions =
                symtab.getInnermostActiveScope() //
                        .query(new MathFunctionNamedQuery(qualifier, name))
                        .stream()
                        .filter(s -> s.getType() instanceof MTFunction)
                        .collect(Collectors.toList());

        List<MTType> sameNameFunctionTypes = sameNameFunctions.stream()
                .map(MathSymbol::getType).collect(Collectors.toList());

        if (sameNameFunctions.isEmpty()) {
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_MATH_FUNCTION,
                    ctx.getStart(), name.getText());
        }
        MathSymbol intendedFunction = null;
        try {
            intendedFunction = getExactDomainTypeMatch(e, sameNameFunctions);
        }
        catch (NoSolutionException nsee) {
            try {
                intendedFunction = getInexactDomainTypeMatch(e, sameNameFunctions);
            }
            catch (NoSolutionException nsee2) {
                compiler.errorManager.semanticError(ErrorKind.NO_MATH_FUNC_FOR_DOMAIN,
                        ctx.getStart(), eType.getDomain(), sameNameFunctions,
                        sameNameFunctionTypes);
            }
        }
        if (intendedFunction == null) return null;
        MTFunction intendedEntryType = (MTFunction) intendedFunction.getType();

        compiler.info("matching " + name.getText() + " : " + eType
                + " to " + intendedFunction.getName() + " : " + intendedEntryType);

        return intendedFunction;
    }

    private MathSymbol getExactDomainTypeMatch(PSymbol e,
            List<MathSymbol> candidates) throws NoSolutionException {
        return getDomainTypeMatch(e, candidates, EXACT_DOMAIN_MATCH);
    }

    private MathSymbol getInexactDomainTypeMatch(PSymbol e,
            List<MathSymbol> candidates) throws NoSolutionException {
        return getDomainTypeMatch(e, candidates, INEXACT_DOMAIN_MATCH);
    }

    private MathSymbol getDomainTypeMatch(PSymbol e,
            List<MathSymbol> candidates,
            TypeComparison<PSymbol, MTFunction> comparison)
            throws NoSolutionException {
        MTFunction eType = e.getConservativePreApplicationType(g);
        MathSymbol match = null;

        MTFunction candidateType;
        for (MathSymbol candidate : candidates) {
            try {
                candidate =
                        candidate.deschematize(e.getArguments(),
                                symtab.getInnermostActiveScope());
                candidateType = (MTFunction) candidate.getType();
                compiler.info(candidate.getType() + " deschematizes to "
                        + candidateType);

                if ( comparison.compare(e, eType, candidateType) ) {
                    if ( match != null ) {
                        compiler.errorManager.semanticError(
                                ErrorKind.AMBIGIOUS_DOMAIN,
                                ((ParserRuleContext) candidate
                                        .getDefiningTree()).getStart(), match
                                        .getName(), match.getType(), candidate
                                        .getName(), candidate.getType());
                    }
                    match = candidate;
                }
            }
            catch (NoSolutionException nse) {
                //couldn't deschematize--try the next one
                compiler.info(candidate.getType() + " doesn't deschematize "
                        + "against " + e.getArguments());
            }
        }
        if ( match == null ) {
            throw NoSolutionException.INSTANCE;
        }
        return match;
    }

    private static class ExactParameterMatch implements Comparator<MTType> {

        @Override public int compare(MTType o1, MTType o2) {
            int result;
            if ( o1.equals(o2) ) {
                result = 0;
            }
            else {
                result = 1;
            }
            return result;
        }
    }

    private static class ExactDomainMatch
            implements
                TypeComparison<PSymbol, MTFunction> {
        @Override public boolean compare(PSymbol foundValue,
                MTFunction foundType, MTFunction expectedType) {
            return foundType.parameterTypesMatch(expectedType,
                    EXACT_PARAMETER_MATCH);
        }

        @Override public String description() {
            return "exact";
        }
    }

    private class InexactDomainMatch
            implements
                TypeComparison<PSymbol, MTFunction> {

        @Override public boolean compare(PSymbol foundValue,
                MTFunction foundType, MTFunction expectedType) {
            return expectedType.parametersMatch(foundValue.getArguments(),
                    INEXACT_PARAMETER_MATCH);
        }

        @Override public String description() {
            return "inexact";
        }
    }

    private class InexactParameterMatch implements TypeComparison<PExp, MTType> {

        @Override public boolean compare(PExp foundValue, MTType foundType,
                MTType expectedType) {
            return g.isKnownToBeIn(foundValue, expectedType);
        }

        @Override public String description() {
            return "inexact";
        }
    }

    protected final void chainMathTypes(ParseTree current, ParseTree child) {
        tr.mathTypes.put(current, tr.mathTypes.get(child));
        tr.mathTypeValues.put(current, tr.mathTypeValues.get(child));
    }

    protected final PExp getPExpFor(@Nullable ParseTree ctx) {
        if ( ctx == null ) {
            return g.getTrueExp();
        }
        PExpBuildingListener<PExp> builder =
                new PExpBuildingListener<>(symtab.mathPExps,
                        symtab.quantifiedExps, tr);
        ParseTreeWalker.DEFAULT.walk(builder, ctx);
        return builder.getBuiltPExp(ctx);
    }

    protected final String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }
}
