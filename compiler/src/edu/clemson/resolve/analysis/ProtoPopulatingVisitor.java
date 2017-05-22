package edu.clemson.resolve.analysis;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.analysis.ProtoTypeSystem.SymbolTable.*;
import edu.clemson.resolve.analysis.ProtoTypeSystem.Types.*;
import edu.clemson.resolve.compiler.*;
import edu.clemson.resolve.parser.*;
import edu.clemson.resolve.semantics.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;

import java.util.*;

public class ProtoPopulatingVisitor extends ResolveBaseVisitor<Void> {
    private static final boolean DEBUG_ENABLED = true;

    private final RESOLVECompiler myCompiler;
    private final SymbolTable mySymbolTable;
    private final AnnotatedModule mySyntaxTree;
    private final Map<ParserRuleContext, SymbolTableEntry> myDecorations;

    public ProtoPopulatingVisitor(RESOLVECompiler compiler, AnnotatedModule syntaxTree) {
        myCompiler = compiler;
        mySymbolTable = new SymbolTable();
        mySyntaxTree = syntaxTree;
        myDecorations = new HashMap<>();

        // Initialize built-in types
        try {
            mySymbolTable.addEntry("Cls", MTEntity.HYPERSET, MTEntity.CLS);
            mySymbolTable.addEntry("SSet", MTEntity.CLS, MTEntity.SSET);
            mySymbolTable.addEntry("B", MTEntity.SSET, MTEntity.BOOLEAN);

        } catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, null, null);
        }
    }

    @Override
    public Void visit(ParseTree tree) {
        if (myCompiler.errMgr.getErrorCount() > 0) {
            return null;
        }
        return super.visit(tree);
    }

    @Override
    public Void visitChildren(RuleNode node) {
        if (myCompiler.errMgr.getErrorCount() > 0) {
            return null;
        }
        return super.visitChildren(node);
    }

    private void emitDebug(String msg) {
        if (DEBUG_ENABLED) {
            System.out.println(msg);
        }
    }

    private void emitError(String msg) {
        System.err.println(msg);
    }

    @Override
    public Void visitPrecisModuleDecl(ResolveParser.PrecisModuleDeclContext ctx) {
        emitDebug("BEGIN PRECIS (open scope)");
        mySymbolTable.openScope();
        super.visitChildren(ctx);
        mySymbolTable.closeScope();
        emitDebug("END PRECIS (close scope)");
        return null;
    }

    @Override
    public Void visitMathClssftnExp(ResolveParser.MathClssftnExpContext ctx) {

        try {
            SymbolTableEntry entry = mySymbolTable.getEntry(ctx.getText());
            if (entry.getTypeValue().isKnownType()) {
                myDecorations.put(ctx, entry);
            } else {
                myCompiler.errMgr.semanticError(ErrorKind.INVALID_MATH_TYPE, ctx.getStart(), ctx.getText());
            }
        } catch (NoSuchSymbolException e) {
            myCompiler.errMgr.semanticError(ErrorKind.NO_SUCH_SYMBOL, ctx.getStart(), ctx.getText());
        }
        return null;
    }

    @Override
    public Void visitMathPrefixDefnSig(ResolveParser.MathPrefixDefnSigContext ctx) {
        mySymbolTable.openScope();
        visitChildren(ctx);
        mySymbolTable.closeScope();

        if (myCompiler.errMgr.getErrorCount() == 0) {
            MTEntity type = myDecorations.get(ctx.mathClssftnExp()).getTypeValue();
            ResolveParser.MathSymbolNameContext symCtx = ctx.mathSymbolName(0);

            try {
                if (ctx.mathVarDeclGroup().size() > 0) {
                    List<MTEntity> operandTypes = new LinkedList<>();
                    for (ResolveParser.MathVarDeclGroupContext varDecl : ctx.mathVarDeclGroup()) {
                        operandTypes.add(myDecorations.get(varDecl.mathClssftnExp()).getTypeValue());
                    }
                    mySymbolTable.addEntry(symCtx.getText(), new MTFunction(symCtx.getText(), operandTypes, type));
                } else {
                    mySymbolTable.addEntry(symCtx.getText(), type);
                }
                emitDebug("Added: " + mySymbolTable.getLastEntry());
            } catch (DuplicateSymbolException e) {
                myCompiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, symCtx.getStart(), symCtx.getText());
            }
        }

        return null;
    }

    @Override
    public Void visitMathVarDecl(ResolveParser.MathVarDeclContext ctx) {
        visitChildren(ctx);
        try {
            mySymbolTable.addEntry(ctx.mathSymbolName().getText(), myDecorations.get(ctx.mathClssftnExp()).getTypeValue());
        } catch (DuplicateSymbolException e) {
            myCompiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, ctx.getStart(), ctx.mathSymbolName().getText());
        }
        return null;
    }

    @Override
    public Void visitMathVarDeclGroup(ResolveParser.MathVarDeclGroupContext ctx) {
        visit(ctx.mathClssftnExp());
        MTEntity type = myDecorations.get(ctx.mathClssftnExp()).getTypeValue();
        for (ResolveParser.MathSymbolNameContext symCtx : ctx.mathSymbolName()) {
            try {
                mySymbolTable.addEntry(symCtx.getText(), type);
            } catch (DuplicateSymbolException e) {
                myCompiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL, symCtx.getStart(), symCtx.getText());
            }
        }
        return null;
    }

    @Override
    public Void visitMathPrimaryExp(ResolveParser.MathPrimaryExpContext ctx) {
        try {
            myDecorations.put(ctx, mySymbolTable.getEntry(ctx.getText()));
        } catch (NoSuchSymbolException e) {
            myCompiler.errMgr.semanticError(ErrorKind.NO_SUCH_SYMBOL, ctx.getStart(), ctx.getText());
        }
        return null;
    }

    @Override
    public Void visitMathTheoremDecl(ResolveParser.MathTheoremDeclContext ctx) {
        mySymbolTable.openScope();
        visitChildren(ctx);
        mySymbolTable.closeScope();
        return null;
    }

    @Override
    public Void visitMathAssertionExp(ResolveParser.MathAssertionExpContext ctx) {
        visitChildren(ctx);
        //MTEntity type = myDecorations.get(ctx.)
        return null;
    }

    @Override
    public Void visitMathPrefixAppExp(ResolveParser.MathPrefixAppExpContext ctx) {
        visitChildren(ctx);
        typeMathFunctionApp(ctx, ctx.name, ctx.mathExp().subList(1, ctx.mathExp().size()));
        return super.visitMathPrefixAppExp(ctx);
    }

    @Override
    public Void visitMathSymbolName(ResolveParser.MathSymbolNameContext ctx) {
        try {
            myDecorations.put(ctx, mySymbolTable.getEntry(ctx.getText()));

        } catch (NoSuchSymbolException e) {
            // do nothing
        }
        return null;
    }

    @Override
    public Void visitMathRecognitionDecl(ResolveParser.MathRecognitionDeclContext ctx) {
        ResolveParser.MathSymbolNameContext symCtx = ctx.mathQuantifiedTypeExp().mathVarDecl().mathSymbolName();
        mySymbolTable.openScope();
        visit(ctx.mathQuantifiedTypeExp().mathVarDeclGroup());
        visit(ctx.mathQuantifiedTypeExp().mathVarDecl().mathClssftnExp());
        try {
            MTEntity quantifiedType = mySymbolTable.getEntry(symCtx.getText()).getType();
            MTEntity assertedType = myDecorations.get(ctx.mathQuantifiedTypeExp().mathVarDecl().mathClssftnExp()).getTypeValue();
            emitDebug(String.format("Recognition found: %s subtype of %s", quantifiedType, assertedType));
            quantifiedType.addKnownSupertype(assertedType);
        } catch (NoSuchSymbolException e) {
            myCompiler.errMgr.semanticError(ErrorKind.NO_SUCH_SYMBOL, symCtx.getStart(), symCtx.getText());
        } finally {
            mySymbolTable.closeScope();
        }
        return null;
    }

    private void typeMathFunctionApp(ParserRuleContext ctx, ParserRuleContext name, List<? extends ParseTree> args) {
        emitDebug("Typing function application: " + ctx.getText());
        try {
            SymbolTableEntry entry = mySymbolTable.getEntry(name.getText());
            if (entry.getType() instanceof MTFunction) {
                boolean paramsMatch = true;
                Iterator<MTEntity> iterExpected = ((MTFunction)entry.getType()).getDomainTypes().iterator();
                Iterator<? extends ParseTree> iterActual = args.iterator();
                while(iterExpected.hasNext() && iterActual.hasNext()) {
                    MTEntity expectedType = iterExpected.next();
                    ParserRuleContext prc = (ParserRuleContext)iterActual.next();
                    MTEntity actualType = myDecorations.get(prc).getType();
                    if (!actualType.isEquivalentOrSubtypeOf(expectedType)) {
                        myCompiler.errMgr.semanticError(ErrorKind.INVALID_APPLICATION_ARG, prc.getStart(), prc.getText(), actualType.toString());
                        paramsMatch = false;
                    } else {
                        emitDebug(String.format("Matched types: %s --> %s", actualType, expectedType));
                    }
                }

                if (iterExpected.hasNext() || iterActual.hasNext()) {
                    myCompiler.errMgr.semanticError(ErrorKind.INCORRECT_FUNCTION_ARG_COUNT, name.getStart(), name.getText());
                    paramsMatch = false;
                }

                if (paramsMatch) {
                    emitDebug("FOUND TYPE: " + entry.getType());
                    myDecorations.put(ctx, entry);
                }
            } else {
                myCompiler.errMgr.semanticError(ErrorKind.APPLYING_NON_FUNCTION, ctx.getStart(), entry.getSymbol());
            }
        } catch (NoSuchSymbolException e) {
            myCompiler.errMgr.semanticError(ErrorKind.NO_SUCH_SYMBOL, ctx.getStart(), ctx.getText());
        }
    }
}
