package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.*;
import org.rsrg.semantics.symbol.MathSymbol;

import java.util.ArrayList;
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
     * (namely, one of the 4 styles of defn signatures therein), this
     * holds a ref to the scope that the defn binding should be added to;
     * is {@code null} otherwise.
     */
    private Scope defnEnclosingScope = null;

    /** This is {@code true} if and only if we're visiting  ctxs on the right
     *  hand side of a colon (<tt>:</tt>); {@code false} otherwise.
     */
    private boolean walkingType = false;

    private boolean walkingDefnParams = false;

    /** A mapping from {@code ParserRuleContext}s to their corresponding
     *  {@link MathType}s; only applies to exps.
     */
    public ParseTreeProperty<MathType> mathTypes =
            new ParseTreeProperty<>();
    public ParseTreeProperty<MathType> exactNamedIntermediateMathTypes =
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
                                         @NotNull List<? extends ParseTree> names)
            throws DuplicateSymbolException {
        //first visit the formal params
        walkingDefnParams = true;
        formals.forEach(this::visit);
        walkingDefnParams = false;

        //next, visit the definition's 'return type' to give it a type
        this.visit(type);
        MathType colonRhsType = exactNamedIntermediateMathTypes.get(type);

        String x = names.get(0).getText();
        MathType defnType = null;
        if (colonRhsType.typeRefDepth > 0) {
            int newTypeDepth = colonRhsType.typeRefDepth - 1;
            List<MathType> paramTypes = new ArrayList<>();
            //List<String> paramNames = new ArrayList<>();

            if (!formals.isEmpty()) {
                for (ParseTree formal : formals) {
                    ResolveParser.MathVarDeclGroupContext grp =
                            (ResolveParser.MathVarDeclGroupContext) formal;
                    for (TerminalNode t : grp.ID()) {
                        MathType ty = exactNamedIntermediateMathTypes.get(grp.mathTypeExp());
                        paramTypes.add(ty);
                        //paramNames.add(t.getText());
                    }
                }
                defnType = new MathFunctionType(g, colonRhsType, paramTypes);

                for (ParseTree t : names) {
                    MathType asNamed = new MathNamedType(g, t.getText(),
                            newTypeDepth, defnType);
                    defnEnclosingScope
                            .define(new MathSymbol(g, t.getText(), asNamed));
                }
                /*for (TerminalNode t : names) {
                    defnEnclosingScope
                            .define(new MathSymbol(g, t.getText(), defnType));
                }*/
            } else {
                for (ParseTree t : names) {
                    defnType = new MathNamedType(g, t.getText(),
                            newTypeDepth, colonRhsType);
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

    private ModuleIdentifier getRootModuleIdentifier() {
        return symtab.getInnermostActiveScope().getModuleIdentifier();
    }

    private void noSuchModule(NoSuchModuleException nsme) {
        compiler.errMgr.semanticError(ErrorKind.NO_SUCH_MODULE,
                nsme.getRequestedModule(),
                nsme.getRequestedModule().getText());
    }

}
