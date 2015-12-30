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

    @Override public void enterConceptImplModuleDecl(
            ResolveParser.ConceptImplModuleDeclContext ctx) {
        tr.uses.add(new ModuleIdentifier(ctx.concept));
        tr.semanticallyRelevantUses.add(new ModuleIdentifier(ctx.concept));
    }

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

    @Override public void enterConceptExtModuleDecl(
            ResolveParser.ConceptExtModuleDeclContext ctx) {
        tr.uses.add(new ModuleIdentifier(ctx.concept));
        tr.semanticallyRelevantUses.add(new ModuleIdentifier(ctx.concept));
    }

    @Override public void enterConceptExtImplModuleDecl(
            ResolveParser.ConceptExtImplModuleDeclContext ctx) {
        tr.uses.add(new ModuleIdentifier(ctx.extension));
        tr.uses.add(new ModuleIdentifier(ctx.concept));
        tr.semanticallyRelevantUses.add(new ModuleIdentifier(ctx.extension));
        tr.semanticallyRelevantUses.add(new ModuleIdentifier(ctx.concept));
    }

    @Override public void exitUsesList(ResolveParser.UsesListContext ctx) {
        for (TerminalNode t : ctx.ID()) {
            tr.uses.add(new ModuleIdentifier(t.getSymbol()));
            tr.semanticallyRelevantUses.add(
                    new ModuleIdentifier(t.getSymbol()));
        }
    }

    @Override public void exitFacilityDecl(
            ResolveParser.FacilityDeclContext ctx) {
        tr.uses.add(new ModuleIdentifier(ctx.spec));
        //tr.semanticallyRelevantUses.add(ctx.spec.getText());
        if ( ctx.externally != null ) {
            tr.externalUses.put(ctx.impl.getText(),
                    new ModuleIdentifier(ctx.impl));
        }
        else {
            tr.uses.add(new ModuleIdentifier(ctx.impl));
        }
    }

    @Override public void exitExtensionPairing(
            ResolveParser.ExtensionPairingContext ctx) {
        tr.uses.add(new ModuleIdentifier(ctx.spec));
        if ( ctx.externally != null ) {
            tr.externalUses.put(ctx.impl.getText(),
                    new ModuleIdentifier(ctx.impl));
        }
        else {
            tr.uses.add(new ModuleIdentifier(ctx.impl));
        }
    }
}