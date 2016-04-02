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
     * (namely, one of the four styles of defn signatures therein), this
     * holds a ref to the scope that the defn binding should be added to;
     * holds {@code null} otherwise.
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
    public ParseTreeProperty<MathClassification> mathTypes =
            new ParseTreeProperty<>();
    public ParseTreeProperty<MathClassification> exactNamedIntermediateMathTypes =
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
        MathClassification colonRhsType = exactNamedIntermediateMathTypes.get(type);

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
                            MathClassification ty = exactNamedIntermediateMathTypes.get(grp.mathTypeExp());
                            paramTypes.add(ty);
                            //paramNames.add(t.getText());
                        }
                    }
                    catch (ClassCastException cce) {
                        ResolveParser.MathVarDeclContext singularDecl =
                                (ResolveParser.MathVarDeclContext) formal;
                            MathClassification ty = exactNamedIntermediateMathTypes.get(singularDecl.mathTypeExp());
                            paramTypes.add(ty);
                    }
                }
                defnType = new MathArrowClassification(g, colonRhsType, paramTypes);

                for (ParseTree t : names) {
                    MathClassification asNamed = new MathNamedClassification(g, t.getText(),
                            newTypeDepth, defnType);
                    if (asNamed.typeRefDepth < 1) {
                        defnType = colonRhsType;
                    }
                    defnEnclosingScope
                            .define(new MathSymbol(g, t.getText(), asNamed));
                }
            } else {
                for (ParseTree t : names) {
                    defnType = new MathNamedClassification(g, t.getText(),
                            newTypeDepth, colonRhsType);
                    if (defnType.typeRefDepth < 1) {
                        defnType = colonRhsType;
                    }
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
        MathClassification rhsColonType = exactNamedIntermediateMathTypes.get(t);
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

        MathClassification type = exactNamedIntermediateMathTypes.get(ctx.mathExp());
        if (type == g.INVALID || type == null || type.typeRefDepth == 0) {
            compiler.errMgr.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
            type = g.INVALID;
        }
        exactNamedIntermediateMathTypes.put(ctx, type);
        mathTypes.put(ctx, type.enclosingClassification);
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
        visitAndClassifyMathExpCtx(ctx, ctx.mathExp());
        return null;
    }

    @Override public Void visitMathInfixAppExp(
            ResolveParser.MathInfixAppExpContext ctx) {
        System.out.println("visiting: " + ctx.getText());
        String x = ctx.getText();
        typeMathFunctionApp(ctx, (ParserRuleContext) ctx.getChild(1),
                ctx.mathExp());
        return null;
    }

    @Override public Void visitMathPrefixAppExp(
            ResolveParser.MathPrefixAppExpContext ctx) {
        typeMathFunctionApp(ctx, ctx.name,
                ctx.mathExp().subList(1, ctx.mathExp().size()));
        return null;
    }

    private void typeMathFunctionApp(@NotNull ParserRuleContext ctx,
                                     @NotNull ParserRuleContext nameExp,
                                     @NotNull ParseTree... args) {
        typeMathFunctionApp(ctx, nameExp, Arrays.asList(args));
    }

    private void typeMathFunctionApp(@NotNull ParserRuleContext ctx,
                                     @NotNull ParserRuleContext nameExp,
                                     @NotNull List<? extends ParseTree> args) {
        this.visit(nameExp);
        args.forEach(this::visit);
        String asString = ctx.getText();
        MathClassification t = exactNamedIntermediateMathTypes.get(nameExp);
        //if we're a name identifying a function, get our function type.
        if (t instanceof MathNamedClassification && t.getEnclosingClassification() instanceof MathArrowClassification) {
            t = ((MathNamedClassification) t).enclosingClassification;
        }
        if (!(t instanceof MathArrowClassification)) {
            compiler.errMgr.semanticError(ErrorKind.APPLYING_NON_FUNCTION,
                    nameExp.getStart(), nameExp.getText());
            exactNamedIntermediateMathTypes.put(ctx, g.INVALID);
            mathTypes.put(ctx, g.INVALID);
            return;
        }
        MathArrowClassification expectedFuncType = (MathArrowClassification) t;
        List<MathClassification> actualArgumentTypes = Utils.apply(args, mathTypes::get);
        List<MathClassification> formalParameterTypes =
                MathSymbol.getParameterTypes((MathArrowClassification) expectedFuncType);
        String applicationText = ctx.getText();

        if (formalParameterTypes.size() != actualArgumentTypes.size()) {
            compiler.errMgr.semanticError(ErrorKind.INCORRECT_FUNCTION_ARG_COUNT,
                    ctx.getStart(), ctx.getText());
            exactNamedIntermediateMathTypes.put(ctx, g.INVALID);
            mathTypes.put(ctx, g.INVALID);
            return;
        }
        try {
            expectedFuncType = (MathArrowClassification)
                    expectedFuncType.deschematize(actualArgumentTypes);
        } catch (BindingException e) {
            System.out.println("formal params in: '" + asString +
                    "' don't bind against the actual arg types");
        }
        //we have to redo this since deschematize above might've changed the
        //args
        formalParameterTypes = MathSymbol.getParameterTypes(
                (MathArrowClassification) expectedFuncType);

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
            exactNamedIntermediateMathTypes.put(ctx, g.INVALID);
            mathTypes.put(ctx, g.INVALID);
        }
        else if (walkingType) {
            List<MathClassification> actualNamedArgumentTypes =
                    Utils.apply(args, exactNamedIntermediateMathTypes::get);
            MathClassification appType = null;
            if (nameExp.getText().equals("⟶")) {
                 appType = expectedFuncType
                        .getApplicationType("⟶", actualNamedArgumentTypes);
            }
            else {
                appType = new MathArrowClassification(g,
                        expectedFuncType.getResultType(),
                        actualNamedArgumentTypes);
            }

            exactNamedIntermediateMathTypes.put(ctx, appType);
            mathTypes.put(ctx, appType);
        } else {
            //the math type of an application is the range, according to the rule:
            // C \ f : C x D -> R
            // C \ E1 : C
            // C \ E2 : D
            // ---------------------
            // C \ f(E1, E2) : R
            exactNamedIntermediateMathTypes.put(ctx, expectedFuncType.getResultType());
            mathTypes.put(ctx, expectedFuncType.getResultType());
        }
    }

    @Override public Void visitMathBooleanOpExp(
            ResolveParser.MathBooleanOpExpContext ctx) {
        exactNamedIntermediateMathTypes.put(ctx, g.BOOLEAN_FUNCTION);
        mathTypes.put(ctx, g.BOOLEAN_FUNCTION);
        return null;
    }

    @Override public Void visitMathAddOpExp(
            ResolveParser.MathAddOpExpContext ctx) {
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }

    @Override public Void visitMathImpliesOpExp(
            ResolveParser.MathImpliesOpExpContext ctx) {
        //exactNamedIntermediateMathTypes.put(ctx, g.BOOLEAN_FUNCTION);
        //mathTypes.put(ctx, g.BOOLEAN_FUNCTION);
        typeMathSymbol(ctx, ctx.qualifier, ctx.ID().getText());
        return null;
    }

    @Override public Void visitMathEqualityOpExp(
            ResolveParser.MathEqualityOpExpContext ctx) {
        //exactNamedIntermediateMathTypes.put(ctx, g.EQUALITY_FUNCTION);
        //mathTypes.put(ctx, g.EQUALITY_FUNCTION);
        typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
        return null;
    }

    @Override public Void visitMathArrowOpExp(
            ResolveParser.MathArrowOpExpContext ctx) {
        exactNamedIntermediateMathTypes.put(ctx, g.ARROW_FUNCTION);
        mathTypes.put(ctx, g.ARROW_FUNCTION);
        //typeMathSymbol(ctx, ctx.qualifier, ctx.op.getText());
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

    private void typeMathSymbol(@NotNull ParserRuleContext ctx,
                                @Nullable Token qualifier,
                                @NotNull String name) {
        MathSymbol s = getIntendedMathSymbol(qualifier, name, ctx);
        if (s == null || s.getMathType() == null) {
            exactNamedIntermediateMathTypes.put(ctx, g.INVALID);
            mathTypes.put(ctx, g.INVALID);
            return;
        }
        String here = ctx.getText();
        exactNamedIntermediateMathTypes.put(ctx, s.getMathType());
        if (s.getMathType().identifiesSchematicType) {
            mathTypes.put(ctx, s.getMathType());
        }
        else if (walkingType) {
            mathTypes.put(ctx, s.getMathType().enclosingClassification);
        }
        else {
            mathTypes.put(ctx, s.getMathType());
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
        MathClassification foundType = mathTypes.get(ctx);
        if (!foundType.equals(expected) &&
                !g.isSubtype(foundType, expected)) {
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
        exactNamedIntermediateMathTypes.put(ctx,
                exactNamedIntermediateMathTypes.get(child));
        mathTypes.put(ctx, mathTypes.get(child));
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
