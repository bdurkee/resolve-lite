package edu.clemson.resolve.compiler;

import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
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
            Resolve.ConceptImplModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
        tr.semanticallyVisibleUses.add(ctx.concept.getText());
    }

    @Override public void enterEnhancementModule(
            Resolve.EnhancementModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
        tr.semanticallyVisibleUses.add(ctx.concept.getText());
    }

    @Override public void enterEnhancementImplModule(
            Resolve.EnhancementImplModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.enhancement));
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
        tr.semanticallyVisibleUses.add(ctx.enhancement.getText());
        tr.semanticallyVisibleUses.add(ctx.concept.getText());
    }

    @Override public void enterPrecisExtensionModule(
            Resolve.PrecisExtensionModuleContext ctx) {
        for (TerminalNode t : ctx.ID()) { // this will automatically add <id> + 'for' <id> + extended by <id>s
            tr.uses.add(new AnnotatedTree.UsesRef(ctx.precis));
            tr.semanticallyVisibleUses.add(ctx.precis.getText());
        }
    }

    @Override public void exitUsesList(Resolve.UsesListContext ctx) {
        tr.uses.addAll(ctx.ID().stream()
                .map(t -> new AnnotatedTree.UsesRef(t.getSymbol()))
                .collect(Collectors.toList()));
        tr.semanticallyVisibleUses.addAll(ctx.ID().stream()
                .map(ParseTree::getText).collect(Collectors.toList()));
    }

    @Override public void exitFacilityDecl(Resolve.FacilityDeclContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.spec));
        //tr.semanticallyVisibleUses.add(ctx.spec.getText());
        if ( ctx.externally != null ) {
            tr.externalUses.put(ctx.impl.getText(),
                    new AnnotatedTree.UsesRef(ctx.impl));
        }
        else {
            tr.uses.add(new AnnotatedTree.UsesRef(ctx.impl));
        }
    }

    @Override public void exitEnhancementPairDecl(
            Resolve.EnhancementPairDeclContext ctx) {
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