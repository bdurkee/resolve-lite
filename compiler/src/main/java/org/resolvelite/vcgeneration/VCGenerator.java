package org.resolvelite.vcgeneration;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.compiler.tree.ImportCollection;
import org.resolvelite.parsing.ResolveBaseListener;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.semantics.ModuleScopeBuilder;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.SymbolTable;

import java.util.List;
import java.util.Set;

public class VCGenerator extends ResolveBaseListener {

    private ModuleScopeBuilder curModuleScope = null;
    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final ResolveCompiler compiler;

    public VCGenerator(@NotNull ResolveCompiler compiler,
            @NotNull SymbolTable symtab, @NotNull AnnotatedTree tree) {
        this.compiler = compiler;
        this.symtab = symtab;
        this.tr = tree;
    }

    @Override public void enterFacilityModule(
            @NotNull ResolveParser.FacilityModuleContext ctx) {
        try {
            curModuleScope =
                    symtab.getModuleScope(ctx.name.getText());

            Set<String> referencedSpecs =
                    tr.imports.getImportsExcluding(
                            ImportCollection.ImportType.EXTERNAL);

        } catch (NoSuchSymbolException e) {
            compiler.errorManager.semanticError(ErrorKind.NO_SUCH_MODULE,
                    ctx.name, ctx.name.getText());
        }
    }

    private List<PExp> getReferencedSpecificationConstraints(AnnotatedTree t) {

        //List<String>

    }

}
