package edu.clemson.resolve.compiler;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.misc.FileLocator;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static edu.clemson.resolve.RESOLVECompiler.NON_NATIVE_EXTENSION;

/**
 * Updates the containers tracking uses reference info by visiting the various {@link ParseTree} nodes that include
 * references to other modules.
 */
public class UsesListener extends ResolveBaseListener {

    private final RESOLVECompiler compiler;

    public UsesListener(@NotNull RESOLVECompiler rc) {
        this.compiler = rc;
    }

    @Override
    public void exitUsesList(ResolveParser.UsesListContext ctx) {
        //TODO: Handle from clauses.
        for (ResolveParser.UsesSpecContext u : ctx.usesSpec()) {

            for (TerminalNode t : u.ID()) {
                ModuleIdentifier id = new ModuleIdentifier(t.getSymbol());
                tr.uses.add(id);
                tr.semanticallyRelevantUses.add(id);
            }
        }
    }

    @Nullable public static File resolveImport(RESOLVECompiler compiler, String usesId, String fromPath) {
        //first check to see if we're on RESOLVEPATH
        Path projectPath = Paths.get(compiler.libDirectory).toAbsolutePath();
        Path resolvePath = Paths.get(RESOLVECompiler.getLibrariesPathDirectory()).toAbsolutePath();
        File result = null;
        try {
            //user specified a root with the fromclause.
            if (!fromPath.equals("")) {
                //a fromclause can either describe something on RESOLVEROOT or it can describe the root
                //of some other resolve project on RESOLVEPATH
            }
            else {
                //search the current project
                result = searchProjectRootDirectory(compiler, usesId);
                //then search the std libs.. if we didn't find anything
                if (result == null) result = searchStdRootDirectory(compiler, usesId);
            }
        }
        catch (IOException e) {
            compiler.errMgr.toolError(ErrorKind.MISSING_IMPORT_FILE, usesId);
        }
        return result;
    }

    @Nullable
    private static File searchProjectRootDirectory(RESOLVECompiler compiler, String usesId) throws IOException {
        Path projectPath = Paths.get(compiler.libDirectory).toAbsolutePath();
        FileLocator l = new FileLocator(usesId, RESOLVECompiler.NATIVE_EXTENSION, "gen", "out");
        Files.walkFileTree(projectPath, l);
        return l.getFile();
    }

    @Nullable
    private static File searchStdRootDirectory(RESOLVECompiler compiler, String usesId) throws IOException {
        Path stdLibPath = Paths.get(RESOLVECompiler.getCoreLibraryDirectory() + File.separator + "src");
        FileLocator l = new FileLocator(usesId, RESOLVECompiler.NATIVE_EXTENSION, "gen", "out");
        Files.walkFileTree(stdLibPath, l);
        return l.getFile();
    }

    @Nullable
    private File searchNonPathProjectDirectory(String fileName) {
        Path projectPath = Paths.get(libDirectory).toAbsolutePath();
        String groomedFileName = Utils.groomFileName(fileName);
        File localFile = new File(projectPath.getParent().toString(), groomedFileName);    //see if it exists in localFile
        return localFile.exists() ? localFile : null;
    }

/*
    @Override
    public void enterPrecisExtModuleDecl(ResolveParser.PrecisExtModuleDeclContext ctx) {
        ModuleIdentifier precisRef = new ModuleIdentifier(ctx.precis);
        tr.uses.add(precisRef);
        tr.semanticallyRelevantUses.add(precisRef);

        if (ctx.precisExt != null) {
            ModuleIdentifier withExtRef = new ModuleIdentifier(ctx.precisExt);
            tr.uses.add(withExtRef);
            tr.semanticallyRelevantUses.add(withExtRef);
        }
        if (ctx.precisExt != null) {
            ModuleIdentifier precisExtRef = new ModuleIdentifier(ctx.precisExt);
            tr.uses.add(precisExtRef);
            tr.semanticallyRelevantUses.add(precisExtRef);
        }
    }

    @Override
    public void exitUsesList(ResolveParser.UsesListContext ctx) {
        //TODO: Handle from clauses.
        for (ResolveParser.UsesSpecContext u : ctx.usesSpec()) {
            for (TerminalNode t : u.ID()) {
                ModuleIdentifier id = new ModuleIdentifier(t.getSymbol());
                tr.uses.add(id);
                tr.semanticallyRelevantUses.add(id);
            }
        }
    }

    @Override
    public void enterConceptImplModuleDecl(ResolveParser.ConceptImplModuleDeclContext ctx) {
        tr.uses.add(new ModuleIdentifier(ctx.concept));
        tr.semanticallyRelevantUses.add(new ModuleIdentifier(ctx.concept));
    }

    @Override
    public void enterConceptExtImplModuleDecl(ResolveParser.ConceptExtImplModuleDeclContext ctx) {
        tr.uses.add(new ModuleIdentifier(ctx.extension));
        tr.uses.add(new ModuleIdentifier(ctx.concept));
        tr.semanticallyRelevantUses.add(new ModuleIdentifier(ctx.extension));
        tr.semanticallyRelevantUses.add(new ModuleIdentifier(ctx.concept));
    }

    @Override
    public void enterConceptExtModuleDecl(
            ResolveParser.ConceptExtModuleDeclContext ctx) {
        tr.uses.add(new ModuleIdentifier(ctx.concept));
        tr.semanticallyRelevantUses.add(new ModuleIdentifier(ctx.concept));
    }

    @Override
    public void exitFacilityDecl(
            ResolveParser.FacilityDeclContext ctx) {
        //tr.uses.add(new ModuleIdentifier(ctx.spec));
        //tr.semanticallyRelevantUses.add(ctx.spec.getText());
        if (ctx.externally != null) {
            tr.externalUses.put(ctx.impl.getText(), new ModuleIdentifier(ctx.impl));
        }
        else {
            //tr.uses.add(new ModuleIdentifier(ctx.impl));
        }
    }

    @Override
    public void exitExtensionPairing(
            ResolveParser.ExtensionPairingContext ctx) {
        //tr.uses.add(new ModuleIdentifier(ctx.spec));
        if (ctx.externally != null) {
            tr.externalUses.put(ctx.impl.getText(), new ModuleIdentifier(ctx.impl));
        }
        else {
            //tr.uses.add(new ModuleIdentifier(ctx.impl));
        }
    }*/
}