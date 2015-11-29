package edu.clemson.resolve.compiler;

import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import static edu.clemson.resolve.compiler.AnnotatedModule.*;

/**
 * Updates the containers tracking uses reference info by visiting the
 * various {@link ParseTree} nodes that reference other modules.
 */
public class UsesListener extends ResolveBaseListener {
    private final AnnotatedModule tr;

    public UsesListener(AnnotatedModule tr) {
        this.tr = tr;
    }

 /*   @Override public void enterConceptImplModule(
            Resolve.ConceptImplModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
        tr.semanticallyRelevantUses.add(ctx.concept.getText());
    }*/

    @Override public void enterPrecisExtensionModuleDecl(
            Resolve.PrecisExtensionModuleDeclContext ctx) {
        tr.uses.add(new UsesRef(ctx.precis));
        tr.semanticallyRelevantUses.add(ctx.precis.getText());
        if (ctx.precisExt != null) {
            tr.uses.add(new UsesRef(ctx.precisExt));
            tr.semanticallyRelevantUses.add(ctx.precisExt.getText());
        }
    }

    /*@Override public void enterExtensionImplModule(
            Resolve.ExtensionImplModuleContext ctx) {
        tr.uses.add(new AnnotatedModule.UsesRef(ctx.enhancement));
        tr.uses.add(new AnnotatedModule.UsesRef(ctx.concept));
        tr.semanticallyRelevantUses.add(ctx.enhancement.getText());
        tr.semanticallyRelevantUses.add(ctx.concept.getText());
    }*/

    @Override public void exitUsesItem(Resolve.UsesItemContext ctx) {
        tr.uses.add(new UsesRef(ctx.ID().getSymbol()));
        tr.semanticallyRelevantUses.add(ctx.ID().getText());

        if (ctx.withExtensions() != null) {
            for (TerminalNode t : ctx.withExtensions().ID()) {
                tr.uses.add(new UsesRef(t.getSymbol()));
                tr.semanticallyRelevantUses.add(t.getText());
            }
        }
    }

  /*  @Override public void exitFacilityDecl(
            Resolve.FacilityDeclContext ctx) {
        tr.uses.add(new AnnotatedModule.UsesRef(ctx.spec));
        //tr.semanticallyRelevantUses.add(ctx.spec.getText());
        if ( ctx.externally != null ) {
            tr.externalUses.put(ctx.impl.getText(),
                    new AnnotatedModule.UsesRef(ctx.impl));
        }
        else {
            tr.uses.add(new AnnotatedModule.UsesRef(ctx.impl));
        }
    }*/

    /*@Override public void exitEnhancementPairDecl(
            Resolve.EnhancementPairDeclContext ctx) {
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