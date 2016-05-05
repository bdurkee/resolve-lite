package edu.clemson.resolve.analysis;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.misc.FileLocator;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveBaseVisitor;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;

class BasicSanityCheckingVisitor extends ResolveBaseVisitor<Void> {

    private final RESOLVECompiler compiler;
    private final AnnotatedModule tr;

    BasicSanityCheckingVisitor(@NotNull RESOLVECompiler compiler, @NotNull AnnotatedModule tr) {
        this.compiler = compiler;
        this.tr = tr;
    }

    @Override
    public Void visitModuleDecl(ResolveParser.ModuleDeclContext ctx) {
        Token moduleNameToken = tr.getNameToken();
        String groomedFileName = Utils.groomFileName(tr.getFileName());
        String extlessFileName = Utils.stripFileExtension(groomedFileName);

        if (!moduleNameToken.getText().equals(extlessFileName)) {
            compiler.errMgr.semanticError(ErrorKind.MODULE_AND_FILE_NAME_DIFFER,
                    moduleNameToken, moduleNameToken.getText(),
                    groomedFileName);
        }
        this.visitChildren(ctx);
        return null;
    }

    @Override
    public Void visitPrecisModuleDecl(ResolveParser.PrecisModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        return null;
    }

    @Override
    public Void visitPrecisExtModuleDecl(ResolveParser.PrecisExtModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        return null;
    }

    @Override
    public Void visitFacilityModuleDecl(ResolveParser.FacilityModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        return null;
    }

    @Override
    public Void visitConceptModuleDecl(ResolveParser.ConceptModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        return null;
    }

    @Override
    public Void visitConceptExtModuleDecl(ResolveParser.ConceptExtModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        return null;
    }

    @Override
    public Void visitConceptExtImplModuleDecl(ResolveParser.ConceptExtImplModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        return null;
    }

    @Override
    public Void visitConceptImplModuleDecl(ResolveParser.ConceptImplModuleDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
        return null;
    }

    @Override
    public Void visitFacilityDecl(ResolveParser.FacilityDeclContext ctx) {
        if (ctx.externally != null) sanityCheckExternalFileRef(ctx.externally);
        return null;
    }

    /**
     * Checks to ensure the name {@link Token}s bookending some scoped block are the same --
     * meaning they contain  the same text.
     */
    private void sanityCheckBlockEnds(@NotNull Token topName, @NotNull Token bottomName) {
        if (!topName.getText().equals(bottomName.getText())) {
            compiler.errMgr.semanticError(ErrorKind.MISMATCHED_BLOCK_END_NAMES, bottomName,
                    topName.getText(), bottomName.getText());
        }
    }

    private void sanityCheckExternalFileRef(@NotNull Token externalNameRef) {
        File externalFile = Utils.getExternalFile(compiler, externalNameRef.getText());
        if (externalFile == null) {
            compiler.errMgr.semanticError(ErrorKind.MISSING_EXTERNAL_FILE, externalNameRef,
                    externalNameRef.getText());
        }
    }
}
