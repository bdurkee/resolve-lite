package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.ImportCollection;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.typereasoning.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.rsrg.semantics.*;
import org.rsrg.semantics.query.MathSymbolQuery;
import org.rsrg.semantics.symbol.MathSymbol;

import java.util.Deque;
import java.util.LinkedList;

public class PopulatingVisitor extends ResolveBaseVisitor<Void> {

    protected RESOLVECompiler compiler;
    protected SymbolTable symtab;
    protected AnnotatedTree tr;
    protected TypeGraph g;

    protected int typeValueDepth = 0;

    /**
     * Any quantification-introducing syntactic node (like, e.g., an
     * {@link edu.clemson.resolve.parser.Resolve.MathQuantifiedExpContext}),
     * introduces a level to this stack to reflect the quantification that
     * should be applied to named variables as they are encountered.
     * <p>
     * Note that this may change as the children of the node are processed;
     * for example, MathVariableDecls found in the declaration portion of a
     * quantified ctx should have quantification (universal or existential)
     * applied, while those found in the body of the quantified ctx QuantExp
     * no quantification (unless there is an embedded quantified ctx). In this
     * case, ctx should not remove its layer, but rather change it to
     * {@code Quantification.NONE}.</p>
     * <p>
     * This stack is never empty, but rather the bottom layer is always
     * {@code Quantification.NONE}.</p>
     */
    private Deque<Quantification> activeQuantifications = new LinkedList<>();

    public PopulatingVisitor(@NotNull RESOLVECompiler rc,
                               @NotNull SymbolTable symtab, AnnotatedTree annotatedTree) {
        this.activeQuantifications.push(Quantification.NONE);
        this.compiler = rc;
        this.symtab = symtab;
        this.tr = annotatedTree;
        this.g = symtab.getTypeGraph();
    }

    @Override public Void visitPrecisModule(
            @NotNull Resolve.PrecisModuleContext ctx) {
        symtab.startModuleScope(ctx, ctx.name.getText()).addImports(
                tr.imports.getImportsOfType(ImportCollection.ImportType.NAMED));
        super.visitChildren(ctx);
        symtab.endScope();
        return null; //java requires return, even if its Void
    }

    /**
     * Note: Shouldn't be calling this.visit(sigature.xxxx) anywhere at this
     * level. Those visits should all be taken care of in the visitor method
     * for the signature itself.
     */
    @Override public Void visitMathDefinitionDecl(
            @NotNull Resolve.MathDefinitionDeclContext ctx) {
        if (ctx.mathDefinitionSig().inductionVar != null) {
            System.err.println("illegal usage of induction variable in standard"
                    + " definition.");
        }
        Resolve.MathDefinitionSigContext sig = ctx.mathDefinitionSig();
        symtab.startScope(ctx);
        this.visit(sig);

        MTType defnType = tr.mathTypes.get(sig);
        MTType defnTypeValue = null;
        if (ctx.mathAssertionExp() != null) {
            //Note: We DO have to visit the rhs assertion explicitly here,
            //as it exists a level above the signature.
            this.visit(ctx.mathAssertionExp());
            defnTypeValue = tr.mathTypeValues.get(ctx.mathAssertionExp());
        }
        symtab.endScope();
        try {
            symtab.getInnermostActiveScope().define(
                    new MathSymbol(g, sig.name.getText(),
                            defnType, defnTypeValue, ctx, getRootModuleID()));
        }
        catch (DuplicateSymbolException e) {
            compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                    sig.name.getStart(), sig.name.getText());
        }
        return null;
    }

    /**
     * Since 'MathDefinitionSig' appears all over the place within our three
     * styles of definitions (categorical, standard, and inductive), we simply
     * use this signature visitor method to visit and type all relevant
     * children. Then, the top level definition nodes can simply grab the type
     * of the signature and build/populate the appropriate object. However,
     * know that in the defn top level nodes, we must remember to start, visit
     * the signature, and end scope. We don't do this in the signature because
     * certain information (rightfully) isn't present -- such as the defn body.
     */
    @Override public Void visitMathDefinitionSig(
            @NotNull Resolve.MathDefinitionSigContext ctx) {
        //first visit the formal params
        activeQuantifications.push(Quantification.UNIVERSAL);
        ctx.mathVariableDeclGroup().forEach(this::visit);
        activeQuantifications.pop();

        //next, visit the definitions 'return type' to give it a type
        this.visit(ctx.mathTypeExp());

        //finally, build the full type of this definitions signature
        //If there are no params, then it is just the sym after the ':'
        MTType defnType = tr.mathTypeValues.get(ctx.mathTypeExp());
        MTFunction.MTFunctionBuilder builder =
                new MTFunction.MTFunctionBuilder(g, defnType);

        //if there are params, then our type needs to be an MTFunction.
        //this if check needs to be here or else, even if there were no params,
        //our type would end up MTFunction: Void -> T (which we don't want)
        if ( !ctx.mathVariableDeclGroup().isEmpty() ) {
            for (Resolve.MathVariableDeclGroupContext grp :
                    ctx.mathVariableDeclGroup()) {
                MTType grpType = tr.mathTypeValues.get(grp.mathTypeExp());
                for (TerminalNode t : grp.ID()) {
                    builder.paramTypes(grpType);
                    builder.paramNames(t.getText());
                }
            }
            //if the definition has parameters then it's type should be an
            //MTFunction a * b ... -> ...
            defnType = builder.build();
        }
        tr.mathTypes.put(ctx, defnType);
        return null;
    }

    @Override public Void visitMathVariableDeclGroup(
            @NotNull Resolve.MathVariableDeclGroupContext ctx) {
        for (TerminalNode t : ctx.ID()) {
            this.visit(ctx.mathTypeExp());
            MTType mathTypeValue = tr.mathTypeValues.get(ctx.mathTypeExp());
            try {
                symtab.getInnermostActiveScope().define(
                        new MathSymbol(g, t.getText(), activeQuantifications
                                .peek(), mathTypeValue, null, ctx,
                                getRootModuleID()));
            }
            catch (DuplicateSymbolException e) {
                compiler.errMgr.semanticError(ErrorKind.DUP_SYMBOL,
                        t.getSymbol(), t.getText());
            }
        }
        return null;
    }

    //---------------------------------------------------
    //  M A T H   E X P   V I S I T O R   M E T H O D S
    //---------------------------------------------------

    @Override public Void visitMathTypeExp(
            @NotNull Resolve.MathTypeExpContext ctx) {
        typeValueDepth++;
        this.visit(ctx.mathExp());
        typeValueDepth--;

        MTType type = tr.mathTypes.get(ctx.mathExp());
        MTType typeValue = tr.mathTypeValues.get(ctx.mathExp());
        if ( typeValue == null ) {
            compiler.errMgr.semanticError(ErrorKind.INVALID_MATH_TYPE,
                    ctx.getStart(), ctx.mathExp().getText());
            typeValue = g.INVALID;
        }
        tr.mathTypes.put(ctx, type);
        tr.mathTypeValues.put(ctx, typeValue);
        return null;
    }

    @Override public Void visitMathBooleanExp(
            @NotNull Resolve.MathBooleanExpContext ctx) {
        exitMathSymbolExp(ctx, null, ctx.getText());
        return null;
    }

    private MathSymbol exitMathSymbolExp(@NotNull ParserRuleContext ctx,
                                         @Nullable Token qualifier, @NotNull String symbolName) {
        MathSymbol intendedEntry = getIntendedEntry(qualifier, symbolName, ctx);

        if ( intendedEntry == null ) {
            tr.mathTypes.put(ctx, g.INVALID);
        }
        else {
            tr.mathTypes.put(ctx, intendedEntry.getType());
            setSymbolTypeValue(ctx, symbolName, intendedEntry);
        }
        return intendedEntry;
    }

    private MathSymbol getIntendedEntry(Token qualifier, String symbolName,
                                        ParserRuleContext ctx) {
        try {
            return symtab.getInnermostActiveScope()
                    .queryForOne(new MathSymbolQuery(qualifier,
                            symbolName, ctx.getStart())).toMathSymbol();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            compiler.errMgr.semanticError(e.getErrorKind(), ctx.getStart(),
                    symbolName);
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
                compiler.errMgr
                        .semanticError(ErrorKind.INVALID_MATH_TYPE,
                                ctx.getStart(), symbolName);
                tr.mathTypeValues.put(ctx, g.INVALID);
            }
        }
    }

    protected final String getRootModuleID() {
        return symtab.getInnermostActiveScope().getModuleID();
    }
}