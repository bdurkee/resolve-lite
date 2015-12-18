package edu.clemson.resolve.compiler;

import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.rsrg.semantics.ModuleIdentifier;

import static edu.clemson.resolve.compiler.AnnotatedModule.*;

/** Updates the containers tracking uses reference info by visiting the
 *  various {@link ParseTree} nodes that include references to other modules.
 */
public class UsesListener extends ResolveBaseListener {
    private final AnnotatedModule tr;

    public UsesListener(AnnotatedModule tr) {
        this.tr = tr;
    }

 /*   @Override public void enterConceptImplModule(
            ResolveParser.ConceptImplModuleContext ctx) {
        tr.uses.add(new AnnotatedTree.UsesRef(ctx.concept));
        tr.semanticallyRelevantUses.add(ctx.concept.getText());
    }*/

    @Override public void enterPrecisExtensionModuleDecl(
            ResolveParser.PrecisExtensionModuleDeclContext ctx) {
        ModuleIdentifier precisRef = new ModuleIdentifier(ctx.precis);
        tr.uses.add(precisRef);
        tr.semanticallyRelevantUses.add(precisRef);
        if (ctx.precisExt != null) {
            ModuleIdentifier precisExtRef = new ModuleIdentifier(ctx.precisExt);
            tr.uses.add(precisExtRef);
            tr.semanticallyRelevantUses.add(precisExtRef);
        }
    }

    /*@Override public void enterExtensionImplModule(
            ResolveParser.ExtensionImplModuleContext ctx) {
        tr.uses.add(new AnnotatedModule.UsesRef(ctx.enhancement));
        tr.uses.add(new AnnotatedModule.UsesRef(ctx.concept));
        tr.semanticallyRelevantUses.add(ctx.enhancement.getText());
        tr.semanticallyRelevantUses.add(ctx.concept.getText());
    }*/

    @Override public void exitUsesList(ResolveParser.UsesListContext ctx) {
        for (TerminalNode t : ctx.ID()) {
            tr.uses.add(new ModuleIdentifier(t.getSymbol()));
            tr.semanticallyRelevantUses.add(
                    new ModuleIdentifier(t.getSymbol()));
        }
    }

  /*  @Override public void exitFacilityDecl(
            ResolveParser.FacilityDeclContext ctx) {
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