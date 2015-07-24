package edu.clemson.resolve.compiler;

import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Updates the containers tracking uses reference info by visiting the
 * various {@link ParseTree} nodes that reference other modules.
 */
public class UsesListener extends ResolveBaseListener {
    private final AnnotatedTree tr;

    public UsesListener(AnnotatedTree tr) {
        this.tr = tr;
    }

    @Override public void enterConceptImplModule(
            @NotNull Resolve.ConceptImplModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
    }

    @Override public void enterEnhancementModule(
            @NotNull Resolve.EnhancementModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
    }

    @Override public void enterEnhancementImplModule(
            @NotNull Resolve.EnhancementImplModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.enhancement));
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
    }

    @Override public void exitUsesList(
            @NotNull Resolve.UsesListContext ctx) {
        tr.uses.addAll(ctx.ID().stream()
                .map(t -> new AnnotatedTree.UsesRef(t.getSymbol()))
                .collect(Collectors.toList()));
    }

    @Override public void exitFacilityDecl(
            @NotNull Resolve.FacilityDeclContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.spec));
        if ( ctx.externally != null ) {
            tr.externalUses.put(ctx.impl.getText(),
                    new AnnotatedTree.UsesRef(ctx.impl));
        }
        else {
            tr.uses.add(new AnnotatedTree.UsesRef(ctx.impl));
        }
    }
}