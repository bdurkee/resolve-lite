package edu.clemson.resolve.compiler;

import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.tree.ParseTree;

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

 /*   @Override public void enterConceptImplModule(
            ResolveParser.ConceptImplModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
        tr.semanticallyRelevantUses.add(ctx.concept.getText());
    }   */

    @Override public void enterExtensionModule(
            ResolveParser.ExtensionModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
        tr.semanticallyRelevantUses.add(ctx.concept.getText());
    }

    @Override public void enterExtensionImplModule(
            ResolveParser.ExtensionImplModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.enhancement));
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
        tr.semanticallyRelevantUses.add(ctx.enhancement.getText());
        tr.semanticallyRelevantUses.add(ctx.concept.getText());
    }

    @Override public void exitUsesList(ResolveParser.UsesListContext ctx) {
        tr.uses.addAll(ctx.ID().stream()
                .map(t -> new AnnotatedTree.UsesRef(t.getSymbol()))
                .collect(Collectors.toList()));
        tr.semanticallyRelevantUses.addAll(ctx.ID().stream()
                .map(ParseTree::getText).collect(Collectors.toList()));
    }

    @Override public void exitFacilityDecl(
            ResolveParser.FacilityDeclContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.spec));
        //tr.semanticallyRelevantUses.add(ctx.spec.getText());
        if ( ctx.externally != null ) {
            tr.externalUses.put(ctx.impl.getText(),
                    new AnnotatedTree.UsesRef(ctx.impl));
        }
        else {
            tr.uses.add(new AnnotatedTree.UsesRef(ctx.impl));
        }
    }

    /*@Override public void exitEnhancementPairDecl(
            ResolveParser.EnhancementPairDeclContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.spec));
        if ( ctx.externally != null ) {
            tr.externalUses.put(ctx.impl.getText(),
                    new AnnotatedTree.UsesRef(ctx.impl));
        }
        else {
            tr.uses.add(new AnnotatedTree.UsesRef(ctx.impl));
        }
    }*/
}