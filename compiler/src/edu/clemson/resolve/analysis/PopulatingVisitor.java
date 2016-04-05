package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.*;
import org.rsrg.semantics.query.MathSymbolQuery;
import org.rsrg.semantics.symbol.MathSymbol;
import org.rsrg.semantics.symbol.TheoremSymbol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class PopulatingVisitor extends ResolveBaseVisitor<Void> {

    private static final boolean EMIT_DEBUG = true;

    private ModuleScopeBuilder moduleScope = null;

    private final RESOLVECompiler compiler;
    private final MathSymbolTable symtab;

    private final AnnotatedModule tr;
    private final DumbTypeGraph g;

    /** While walking children of an
     *      {@link ResolveParser.MathCategoricalDefnDeclContext} or
     *      {@link ResolveParser.MathStandardDefnDeclContext} or
     *      {@link ResolveParser.MathInductiveDefnDeclContext}
     *  (namely, one of the four styles of defn signatures therein), this
     *  holds a ref to the scope that the defn binding should be added to;
     *  holds {@code null} otherwise.
     */
    private Scope defnEnclosingScope = null;

    /** This is {@code true} if and only if we're visiting  ctxs on the right
     *  hand side of a colon (<tt>:</tt>); {@code false} otherwise.
     */
    private boolean walkingType = false;
    private boolean walkingDefnParams = false;

    /** A mapping from {@code ParserRuleContext}s to their corresponding
     *  {@link MathClassification}s; only applies to exps.
     */
    public ParseTreeProperty<MathClassification> mathClassifications =
            new ParseTreeProperty<>();
    public ParseTreeProperty<MathClassification> exactNamedIntermediateMathClassifications =
            new ParseTreeProperty<>();

    public PopulatingVisitor(@NotNull RESOLVECompiler rc,
                             @NotNull MathSymbolTable symtab,
                             @NotNull AnnotatedModule annotatedTree) {
        this.compiler = rc;
        this.symtab = symtab;
        this.tr = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    public DumbTypeGraph getTypeGraph() {
        return g;
    }

    @Override public Void visitModuleDecl(ResolveParser.ModuleDeclContext ctx) {
        moduleScope = symtab.startModuleScope(tr)
                .addImports(tr.semanticallyRelevantUses);
        super.visitChildren(ctx);
        symtab.endScope();
        return null; //java requires a return, even if its 'Void'
    }

    @Override public Void visitPrecisExtModuleDecl(
            ResolveParser.PrecisExtModuleDeclContext ctx) {
        try {
            //exts implicitly gain the parenting precis's useslist
            ModuleScopeBuilder conceptScope = symtab.getModuleScope(
                    new ModuleIdentifier(ctx.precis));
            moduleScope.addImports(conceptScope.getImports());
            moduleScope.addInheritedModules(new ModuleIdentifier(ctx.precis));
        } catch (NoSuchModuleException e) {
            compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                    ctx.precis, ctx.precis.getText());
        }
        super.visitChildren(ctx);
        return null;
    }

    @Override public Void visitMathTheoremDecl(
            ResolveParser.MathTheoremDeclContext ctx) {
        symtab.startScope(ctx);
        this.visit(ctx.mathAssertionExp());
        symtab.endScope();
        MathClassification x = mathClassifications.get(ctx.mathAssertionExp());
        expectType(ctx.mathAssertionExp(), g.BOOLEAN);
        try {
            //PExp assertion = getPExpFor(ctx.mathAssertionExp());
            symtab.getInnermostActiveScope().define(
                    new TheoremSymbol(g, ctx.name.getText(), g.getTrueExp(),
                            ctx, getRootModuleIdentifier()));
        } catch (DuplicateSymbolException dse) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.name, ctx.name.getText());
        }
        return null;
    }

    @Override public Void visitMathCategoricalDefnDecl(
            ResolveParser.MathCategoricalDefnDeclContext ctx) {
        for (ResolveParser.MathPrefixDefnSigContext sig :
                ctx.mathPrefixDefnSigs().mathPrefixDefnSig()) {
            defnEnclosingScope = symtab.getInnermostActiveScope();
            symtab.startScope(ctx);
            this.visit(sig);
            symtab.endScope();
            defnEnclosingScope = null;
        }
        //visit the predicate that groups together the components of our
        //categorical defn
        this.visit(ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitMathInductiveDefnDecl(
            ResolveParser.MathInductiveDefnDeclContext ctx) {
        defnEnclosingScope = symtab.getInnermostActiveScope();
        symtab.startScope(ctx);
        ResolveParser.MathDefnSigContext sig = ctx.mathDefnSig();
        ParserRuleContext baseCase = ctx.mathAssertionExp(0);
        ParserRuleContext indHypo = ctx.mathAssertionExp(1);

        //note that 'sig' adds a binding for the name to the active scope
        //so baseCase and indHypo will indeed be able to see the symbol we're
        //introducing here.
        this.visit(sig);
        this.visit(baseCase);
        this.visit(indHypo);

        expectType(baseCase, g.BOOLEAN);
        expectType(indHypo, g.BOOLEAN);
        symtab.endScope();
        defnEnclosingScope = null;
        return null;
    }

    @Override public Void visitMathStandardDefnDecl(
            ResolveParser.MathStandardDefnDeclContext ctx) {
        defnEnclosingScope = symtab.getInnermostActiveScope();
        symtab.startScope(ctx);
        this.visit(ctx.mathDefnSig());
        if (ctx.body != null) this.visit(ctx.body);
        symtab.endScope();
        defnEnclosingScope = null;
        return null;
    }

    @Override public Void visitMathDefnSig(
            ResolveParser.MathDefnSigContext ctx) {
        this.visitChildren(ctx);
        return null;
    }

    @Override public Void visitMathInfixDefnSig(
            ResolveParser.MathInfixDefnSigContext ctx) {
        try {
            insertMathDefnSignature(ctx, ctx.mathVarDecl(), ctx.mathTypeExp(),
                    ctx.name);
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), e.getOffendingSymbol().getName());
        }
        return null;
    }

    @Override public Void visitMathPrefixDefnSig(
            ResolveParser.MathPrefixDefnSigContext ctx) {
        try {
            insertMathDefnSignature(ctx, ctx.mathVarDeclGroup(), ctx.mathTypeExp(),
                    ctx.mathSymbolName());
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), e.getOffendingSymbol().getName());
        }
        return null;
    }

    private void insertMathDefnSignature(@NotNull ParserRuleContext ctx,
                                         @NotNull List<? extends ParseTree> formals,
                                         @NotNull ResolveParser.MathTypeExpContext type,
                                         @NotNull ParseTree ... names)
            throws DuplicateSymbolException {
        insertMathDefnSignature(ctx, formals, type, Arrays.asList(names));
    }

    private void insertMathDefnSignature(@NotNull ParserRuleContext ctx,
                                         @NotNull List<? extends ParseTree> formals,
                                         @NotNull ResolveParser.MathTypeExpContext type,
                                         @NotNull List<? extends ParseTree> names)
            throws DuplicateSymbolException {
        //first visit the formal params
        walkingDefnParams = true;
        formals.forEach(this::visit);
        walkingDefnParams = false;

        //next, visit the definition's 'return type' to give it a type
        this.visit(type);
        MathClassification colonRhsType =
                exactNamedIntermediateMathClassifications.get(type);

        MathClassification defnType = null;
        if (colonRhsType.typeRefDepth > 0) {
            int newTypeDepth = colonRhsType.typeRefDepth - 1;
            List<MathClassification> paramTypes = new ArrayList<>();
            //List<String> paramNames = new ArrayList<>();

            if (!formals.isEmpty()) {
                for (ParseTree formal : formals) {
                    try {
                        ResolveParser.MathVarDeclGroupContext grp =
                                (ResolveParser.MathVarDeclGroupContext) formal;
                        for (TerminalNode t : grp.ID()) {
                            MathClassification ty = exactNamedIntermediateMathClassifications.get(grp.mathTypeExp());
                            paramTypes.add(ty);
                            //paramNames.add(t.getText());
                        }
                    }
                    catch (ClassCastException cce) {
                        ResolveParser.MathVarDeclContext singularDecl =
                                (ResolveParser.MathVarDeclContext) formal;
                            MathClassification ty = exactNamedIntermediateMathClassifications.get(singularDecl.mathTypeExp());
                            paramTypes.add(ty);
                    }
                }
                defnType = new MathFunctionClassification(g, colonRhsType, paramTypes);

                for (ParseTree t : names) {
                    MathClassification asNamed = new MathNamedClassification(g, t.getText(),
                            newTypeDepth, defnType);
                    defnEnclosingScope
                            .define(new MathSymbol(g, t.getText(), asNamed));
                }
            } else {
                for (ParseTree t : names) {
                    defnType = new MathNamedClassification(g, t.getText(),
                            newTypeDepth, colonRhsType);
                    //if (defnType.typeRefDepth < 1) {
                    //    defnType = colonRhsType;
                    //}
                    defnEnclosingScope
                            .define(new MathSymbol(g, t.getText(), defnType));
                }
            }
        } else {
            for (ParseTree t : names) {
                defnEnclosingScope
                        .define(new MathSymbol(g, t.getText(), g.INVALID));
            }
        }
    }

    @Override public Void visitMathVarDeclGroup(
            ResolveParser.MathVarDeclGroupContext ctx) {
        insertMathVarDecls(ctx, ctx.mathTypeExp(), ctx.ID());
        return null;
    }

    @Override public Void visitMathVarDecl(
            ResolveParser.MathVarDeclContext ctx) {
        insertMathVarDecls(ctx, ctx.mathTypeExp(), ctx.ID());
        return null;
    }

    private void insertMathVarDecls(@NotNull ParserRuleContext ctx,
                                    @NotNull ResolveParser.MathTypeExpContext t,
                                    @NotNull TerminalNode... terms) {
        insertMathVarDecls(ctx, t, Arrays.asList(terms));
    }

    private void insertMathVarDecls(@NotNull ParserRuleContext ctx,
                                    @NotNull ResolveParser.MathTypeExpContext t,
                                    @NotNull List<TerminalNode> terms) {
        this.visitMathTypeExp(t);
        MathClassification rhsColonType = exactNamedIntermediateMathClassifications.get(t);
        for (TerminalNode term : terms) {
            MathClassification ty = new MathNamedClassification(g, term.getText(),
                    rhsColonType.typeRefDepth - 1, rhsColonType);

            //ah! so this will keep things like "k" in the spiral examples from
            //being considered schematic types.
            ty.identifiesSchematicType = walkingDefnParams && rhsColonType.typeRefDepth > 1;
            // if (rhsColonType.typeRefDepth > 1) {
            //     defnSchematicTypes.put(term.getText(), rhsColonType);
            // }
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, term.getText(), ty));
            } catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        ctx.getStart(), e.getOffendingSymbol().getName());
            }
        }
    }

    @Override public Void visitMathTypeExp(
            ResolveParser.MathTypeExpContext ctx) {
        walkingType = true;
        this.visit(ctx.mathExp());
        walkingType = false;

        MathClassification type = exactNamedIntermediateMathClassifications.get(ctx.mathExp());
        if (type == g.INVALID || type == null || type.typeRefDepth == 0) {
            compiler.errMgr.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
            type = g.INVALID;
        }
        exactNamedIntermediateMathClassifications.put(ctx, type);
        mathClassifications.put(ctx, type.enclosingClassification);
        return null;
    }

    @Override public Void visitMathClassificationAssertionExp(
            ResolveParser.MathClassificationAssertionExpContext ctx) {
        this.visit(ctx.mathExp());
        MathClassification rhsColonType =
                exactNamedIntermediateMathClassifications.get(ctx.mathExp());
        MathClassification ty =
                new MathNamedClassification(g, ctx.ID().getText(),
                        rhsColonType.typeRefDepth - 1, rhsColonType);
        ty.identifiesSchematicType = true;
        try {
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, ctx.ID().getText(), ty));
        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    ctx.getStart(), e.getOffendingSymbol().getName());
        }
        //defnSchematicTypes.put(ctx.ID().getText(), ty);
        exactNamedIntermediateMathClassifications.put(ctx, ty);
        mathClassifications.put(ctx, ty);
        return null;
    }

    @Override public Void visitMathQuantifiedExp(
            ResolveParser.MathQuantifiedExpContext ctx) {
        symtab.startScope(ctx);
        Quantification quantification;

        /*switch (ctx.q.getType()) {
            case ResolveLexer.FORALL:
                quantification = Quantification.UNIVERSAL;
                break;
            case ResolveLexer.EXISTS:
                quantification = Quantification.EXISTENTIAL;
                break;
            default:
                throw new RuntimeException("unrecognized quantification type: "
                        + ctx.q.getText());
        }*/
        //activeQuantifications.push(quantification);
        this.visit(ctx.mathVarDeclGroup());
        //activeQuantifications.pop();

        //activeQuantifications.push(Quantification.NONE);
        this.visit(ctx.mathAssertionExp());
        //activeQuantifications.pop();
        symtab.endScope();
        mathClassifications.put(ctx, g.BOOLEAN);
        return null;
    }

    @Override public Void visitMathAssertionExp(
            ResolveParser.MathAssertionExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathPrimaryExp(
            ResolveParser.MathPrimaryExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.mathPrimeExp());
        return null;
    }

    @Override public Void visitMathPrimeExp(
            ResolveParser.MathPrimeExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.getChild(0));
        return null;
    }

    @Override public Void visitMathNestedExp(
            ResolveParser.MathNestedExpContext ctx) {
        visitAndClassifyMathExpCtx(ctx, ctx.mathAssertionExp());
        return null;
    }

    @Override public Void visitMathInfixAppExp(
            ResolveParser.MathInfixAppExpContext ctx) {
        typeMathFunctionAppExp(ctx, (ParserRuleContext) ctx.getChild(1),
                ctx.mathExp());
        return null;
    }

    @Override public Void visitMathPrefixAppExp(
            ResolveParser.MathPrefixAppExpContext ctx) {
        typeMathFunctionAppExp(ctx, ctx.name,
                ctx.mathExp().subList(1, ctx.mathExp().size()));
        return null;
    }

    private void typeMathFunctionAppExp(@NotNull ParserRuleContext ctx,
                                        @NotNull ParserRuleContext nameExp,
                                        @NotNull ParseTree... args) {
        typeMathFunctionAppExp(ctx, nameExp, Arrays.asList(args));
    }

    private void typeMathFunctionAppExp(@NotNull ParserRuleContext ctx,
                                        @NotNull ParserRuleContext nameExp,
                                        @NotNull List<? extends ParseTree> args) {
        this.visit(nameExp);
        args.forEach(this::visit);
        String asString = ctx.getText();
        MathClassification t = exactNamedIntermediateMathClassifications.get(nameExp);
        //if we're a name identifying a function, get our function type.
        if (t instanceof MathNamedClassification && t.getEnclosingClassification() instanceof MathFunctionClassification) {
            t = ((MathNamedClassification) t).enclosingClassification;
        }
        if (!(t instanceof MathFunctionClassification)) {
            compiler.errMgr.semanticError(ErrorKind.APPLYING_NON_FUNCTION,
                    nameExp.getStart(), nameExp.getText());
            exactNamedIntermediateMathClassifications.put(ctx, g.INVALID);
            mathClassifications.put(ctx, g.INVALID);
            return;
        }
        MathFunctionClassification expectedFuncType = (MathFunctionClassification) t;
        List<MathClassification> actualArgumentTypes = Utils.apply(args, mathClassifications::get);
        List<MathClassification> formalParameterTypes =
                expectedFuncType.getParamTypes();

        if (formalParameterTypes.size() != actualArgumentTypes.size()) {
            compiler.errMgr.semanticError(ErrorKind.INCORRECT_FUNCTION_ARG_COUNT,
                    ctx.getStart(), ctx.getText());
            exactNamedIntermediateMathClassifications.put(ctx, g.INVALID);
            mathClassifications.put(ctx, g.INVALID);
            return;
        }
        try {
            expectedFuncType = (MathFunctionClassification)
                    expectedFuncType.deschematize(actualArgumentTypes);
        } catch (BindingException e) {
            System.out.println("formal params in: '" + asString +
                    "' don't bind against the actual arg types");
        }
        //we have to redo this since deschematize above might've changed the
        //args
        formalParameterTypes = expectedFuncType.getParamTypes();

        Iterator<MathClassification> actualsIter = actualArgumentTypes.iterator();
        Iterator<MathClassification> formalsIter = formalParameterTypes.iterator();

        //SUBTYPE AND EQUALITY CHECK FOR ARGS HAPPENS HERE
        while (actualsIter.hasNext()) {
            MathClassification actual = actualsIter.next();
            MathClassification formal = formalsIter.next();
            if (!formal.equals(actual)) {
                if (!g.isSubtype(actual, formal)) {
                    System.err.println("for function application: " +
                            ctx.getText() + "; arg type: " + actual +
                            " not acceptable where: " + formal + " was expected");
                }
            }
        }

        //If we're describing a type, then the range (as a result of the function is too broad),
        //so we'll annotate the type of this application with its (verbose) application type.
        //but it's enclosing type will of course still be the range.
        if (walkingType && expectedFuncType.getResultType().getTypeRefDepth() <= 1) {
            exactNamedIntermediateMathClassifications.put(ctx, g.INVALID);
            mathClassifications.put(ctx, g.INVALID);
        }
        else if (walkingType) {
            List<MathClassification> actualNamedArgumentTypes =
                    Utils.apply(args, exactNamedIntermediateMathClassifications::get);
            MathClassification appType =
                    expectedFuncType.getApplicationType(
                            nameExp.getText(), actualNamedArgumentTypes);
            exactNamedIntermediateMathClassifications.put(ctx, appType);
            mathClassifications.put(ctx, appType);
        } else {
            //the math type of an application is the range, according to the rule:
            // C \ f : C x D -> R
            // C \ E1 : C
            // C \ E2 : D
            // ---------------------
            // C \ f(E1, E2) : R
            exactNamedIntermediateMathClassifications.put(ctx, expectedFuncType.getResultType());
            mathClassifications.put(ctx, expectedFuncType.getResultType());
        }
    }
    /*
    mathMultOpExp : (qualifier=ID '::')? op=('*'|'/'|'%') ;
    mathAddOpExp : (qualifier=ID '::')? op=('+'|'-'|'~');
    mathJoiningOpExp : (qualifier=ID '::')? op=('o'|'union'|'∪'|'∪₊'|'intersect'|'∩'|'∩₊');
    mathArrowOpExp : (qualifier=ID '::')? op=('->'|'⟶') ;
    mathRelationalOpExp : (qualifier=ID '::')? op=('<'|'>'|'<='|'≤'|'≤ᵤ'|'>='|'≥');
    mathEqualityOpExp : (qualifier=ID '::')? op=('='|'/='|'≠');
    mathSetContainmentOpExp : (qualifier=ID '::')? op=('is_in'|'is_not_in'|'∈'|'∉');
    mathImpliesOpExp : (qualifier=ID '::')? op='implies';
    mathBooleanOpExp : (qualifier=ID '::')? op=('and'|'or'|'iff');
    */
    @Override public Void visitMathMultOpExp(
            ResolveParser.MathMultOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathAddOpExp(
            ResolveParser.MathAddOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathJoiningOpExp(
            ResolveParser.MathJoiningOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathArrowOpExp(
            ResolveParser.MathArrowOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathRelationalOpExp(
            ResolveParser.MathRelationalOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathEqualityOpExp(
            ResolveParser.MathEqualityOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathSetContainmentOpExp(
            ResolveParser.MathSetContainmentOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathImpliesOpExp(
            ResolveParser.MathImpliesOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathBooleanOpExp(
            ResolveParser.MathBooleanOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathBooleanLiteralExp(
            ResolveParser.MathBooleanLiteralExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }
    @Override public Void visitMathIntegerLiteralExp(
            ResolveParser.MathIntegerLiteralExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.INT().getText());
        return null;
    }

    @Override public Void visitMathSymbolExp(
            ResolveParser.MathSymbolExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.name.getText());
        return null;
    }

    @Override public Void visitMathSetRestrictionExp(
            ResolveParser.MathSetRestrictionExpContext ctx) {
        this.visit(ctx.mathVarDecl());
        this.visit(ctx.mathAssertionExp());
        MathClassification t =
                g.POWERSET_FUNCTION.getApplicationType("Powerset",
                        exactNamedIntermediateMathClassifications.get(
                                ctx.mathVarDecl().mathTypeExp()));
        exactNamedIntermediateMathClassifications.put(ctx, t);
        mathClassifications.put(ctx, t);
        return null;
    }

    private void typeMathSymbol(@NotNull ParserRuleContext ctx,
                                @Nullable Token qualifier,
                                @NotNull String name) {
        MathSymbol s = getIntendedMathSymbol(qualifier, name, ctx);
        if (s == null || s.getMathType() == null) {
            exactNamedIntermediateMathClassifications.put(ctx, g.INVALID);
            mathClassifications.put(ctx, g.INVALID);
            return;
        }
        String here = ctx.getText();
        exactNamedIntermediateMathClassifications.put(ctx, s.getMathType());
        if (s.getMathType().identifiesSchematicType) {
            mathClassifications.put(ctx, s.getMathType());
        }
        else {
            mathClassifications.put(ctx, s.getMathType().getEnclosingClassification());
        }
    }

    @Nullable private MathSymbol getIntendedMathSymbol(
            @Nullable Token qualifier, @NotNull String symbolName,
            @NotNull ParserRuleContext ctx) {
        try {
            return symtab.getInnermostActiveScope()
                    .queryForOne(new MathSymbolQuery(qualifier,
                            symbolName, ctx.getStart()));
        } catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(),
                    symbolName);
        } catch (NoSuchModuleException nsme) {
            compiler.errMgr.semanticError(nsme.getErrorKind(),
                    nsme.getRequestedModule(),
                    nsme.getRequestedModule().getText());
        } catch (UnexpectedSymbolException use) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_SYMBOL,
                    ctx.getStart(), "a math symbol", symbolName,
                    use.getTheUnexpectedSymbolDescription());
        }
        return null;
    }

    private void expectType(ParserRuleContext ctx, MathClassification expected) {
        MathClassification foundType = mathClassifications.get(ctx);
        if (!g.isSubtype(foundType, expected)) {
            compiler.errMgr.semanticError(ErrorKind.UNEXPECTED_TYPE,
                    ctx.getStart(), expected, foundType);
        }
    }

    /** Given some context {@code ctx} and a
     *  {@code child} context; this method visits {@code child} and chains/passes
     *  its found {@link MathClassification} upto {@code ctx}.
     *
     * @param ctx a parent {@code ParseTree}
     * @param child one of {@code ctx}s children
     */
    private void visitAndClassifyMathExpCtx(@NotNull ParseTree ctx,
                                            @NotNull ParseTree child) {
        this.visit(child);
        exactNamedIntermediateMathClassifications.put(ctx,
                exactNamedIntermediateMathClassifications.get(child));
        MathClassification x = mathClassifications.get(child);
        mathClassifications.put(ctx, mathClassifications.get(child));
    }

    private ModuleIdentifier getRootModuleIdentifier() {
        return symtab.getInnermostActiveScope().getModuleIdentifier();
    }

    private void noSuchModule(NoSuchModuleException nsme) {
        compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                nsme.getRequestedModule(),
                nsme.getRequestedModule().getText());
    }
}
