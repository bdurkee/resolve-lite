package edu.clemson.resolve.compiler;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.misc.FileLocator;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import edu.clemson.resolve.semantics.ModuleIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Updates the containers tracking uses reference info by visiting the various {@link ParseTree} nodes that include
 * references to other modules.
 */
public class UsesListener extends ResolveBaseListener {

    private final RESOLVECompiler compiler;
    public final Set<ModuleIdentifier> uses = new HashSet<>();
    public final Set<ModuleIdentifier> extUses = new HashSet<>();
    public Map<String, ModuleIdentifier> aliases = new HashMap<>();

    private final String fileName;

    public UsesListener(@NotNull String fileName, @NotNull RESOLVECompiler rc) {
        this.compiler = rc;
        this.fileName = fileName;
    }

    @Override
    public void exitUsesList(ResolveParser.UsesListContext ctx) {
        for (ResolveParser.ModuleIdentifierSpecContext u : ctx.usesSpecs().moduleIdentifierSpec()) {
            File f = resolveImport(compiler, u);
            if (f == null) {
                compiler.errMgr.semanticError(ErrorKind.MISSING_IMPORT_FILE, u.ID().getSymbol(), u.ID().getText());
                continue;
            }
            ModuleIdentifier e = new ModuleIdentifier(u.ID().getSymbol(), f);
            uses.add(e);
        }
    }

    //TODO: Ok, assume the externally realized file is from the same package as the spec...
    //slight restriction right now.. external uses must be in the same folder as the concept they are
    //externally realizing.
    //eventually I'd like to allow something like this for facilitydecls
    //
    // Facility SF is Stack_Template(Int, 4) from goo
    //      externally implemented by Java_Stk_Impl from goo.ext;
    //
    // this also gives us a nice way of doing short facility modules (without explicit uses lists -- which from's)
    // so right now, because we don't have implicit imports working yet, Stack_Template
    @Override
    public void exitFacilityDecl(ResolveParser.FacilityDeclContext ctx) {
        ResolveParser.ModuleLibraryIdentifierContext specFrom = ctx.specFrom != null ?
                ctx.specFrom.moduleLibraryIdentifier() : null;
        ResolveParser.ModuleLibraryIdentifierContext implFrom = ctx.implFrom != null ?
                ctx.implFrom.moduleLibraryIdentifier() : null;

        resolveAndAddFacilitySpecOrImpl(ctx.spec, false, specFrom);
        resolveAndAddFacilitySpecOrImpl(ctx.impl, ctx.externally != null, implFrom);
    }

    @Override
    public void exitExtensionPairing(ResolveParser.ExtensionPairingContext ctx) {
        ResolveParser.ModuleLibraryIdentifierContext specFrom = ctx.specFrom != null ?
                ctx.specFrom.moduleLibraryIdentifier() : null;
        ResolveParser.ModuleLibraryIdentifierContext implFrom = ctx.implFrom != null ?
                ctx.implFrom.moduleLibraryIdentifier() : null;

        resolveAndAddFacilitySpecOrImpl(ctx.spec, false, specFrom);
        resolveAndAddFacilitySpecOrImpl(ctx.impl, ctx.externally != null, implFrom);
    }

    private void resolveAndAddFacilitySpecOrImpl(@NotNull Token t,
                                                 boolean isExternal,
                                                 @Nullable ResolveParser.ModuleLibraryIdentifierContext from) {
        if (!isExternal) {
            //we're not an external implementation
            File resolve = resolveImport(t, from);

            if (resolve != null) {
                uses.add(new ModuleIdentifier(t, resolve));
            }
            else {
                compiler.errMgr.semanticError(ErrorKind.MISSING_IMPORT_FILE, t, t.getText());
            }
        }
        else {
            //we're an external implementation..
            File resolveExternal = resolveImport(t, from, RESOLVECompiler.NON_NATIVE_FILE_EXTENSION);
            if (resolveExternal != null) {
                extUses.add(new ModuleIdentifier(t, resolveExternal));
            }
            else {
                compiler.errMgr.semanticError(ErrorKind.MISSING_IMPORT_FILE, t, t.getText());
            }
        }
    }

    //so we don't have Path resolveProjRootPath here because our projects should be on RESOLVEPATH anyways..
    @Nullable
    private static Path getAppropriateRootDirectoryForFromClause(@NotNull String fromStem) {
        Path resolveStdRootPath = Paths.get(RESOLVECompiler.getCoreLibraryDirectory() + File.separator + "src");
        Path resolveLibRootPath = Paths.get(RESOLVECompiler.getLibrariesPathDirectory() + File.separator + "src");

        File result = new File(resolveStdRootPath.toString(), fromStem);
        if (result.exists() && result.isDirectory()) return result.toPath();
        result = new File(resolveLibRootPath.toString(), fromStem);
        if (result.exists() && result.isDirectory()) return result.toPath();
        return null;
    }

    @Nullable
    public File resolveImport(@NotNull RESOLVECompiler compiler,
                              @NotNull ResolveParser.ModuleIdentifierSpecContext u) {
        return resolveImport(u.ID().getSymbol(), u.fromClause() != null ?
                u.fromClause().moduleLibraryIdentifier() : null, RESOLVECompiler.NATIVE_FILE_EXTENSION);
    }

    @Nullable
    public File resolveImport(@NotNull Token usesToken,
                              @Nullable ResolveParser.ModuleLibraryIdentifierContext fromPathCtx,
                              @NotNull String ... extensions) {
        List<String> exts = (extensions.length == 0) ?
                Collections.singletonList(RESOLVECompiler.NATIVE_FILE_EXTENSION) :
                Arrays.asList(extensions);
        return resolveImport(usesToken, fromPathCtx, exts);
    }

    @Nullable
    public File resolveImport(@NotNull Token usesToken,
                              @Nullable ResolveParser.ModuleLibraryIdentifierContext fromPathCtx,
                              @NotNull List<String> extensions) {
        //first check to see if we're on RESOLVEPATH
        Path projectPath = null; //Paths.get(compiler.libDirectory).toAbsolutePath();
        Path resolvePath = Paths.get(RESOLVECompiler.getLibrariesPathDirectory()).toAbsolutePath();
        File result = null;
        if (fromPathCtx != null) {
            //a fromclause can either describe something on RESOLVEROOT or it can describe the root
            //of some other resolve project on RESOLVEPATH

            Path s = getAppropriateRootDirectoryForFromClause(fromPathCtx.getText().replace('.', File.separatorChar));
            if (s == null) {
                compiler.errMgr.semanticError(ErrorKind.BAD_FROM_CLAUSE, fromPathCtx.getStart(), fromPathCtx.getText());
                return null;
            }
            try {
                return findFile(s, usesToken.getText(), extensions);
            }
            catch (IOException ioe) {
                return null;
            }
        }
        else {
            //search the current project
            RESOLVECompiler.getProjectRootDirFromFileName(fileName);


            result = searchProjectRootDirectory(extensions, usesToken.getText());
            //now search the
            //then search the std libs.. if we didn't find anything
            if (result == null) result = searchStdRootDirectory(extensions, usesToken.getText());
        }
        return result;
    }

    @Nullable
    private static File searchProjectRootDirectory(List<String> extensions, String id) {
        Path projectPath = null;//Paths.get(compiler.libDirectory).toAbsolutePath();
        if (projectPath.endsWith(".")) {
            projectPath = projectPath.getParent();
        }
        try {
            return findFile(projectPath, id, extensions);
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    private static File searchStdRootDirectory(List<String> extensions, String id) {
        Path stdLibPath = Paths.get(RESOLVECompiler.getCoreLibraryDirectory() + File.separator + "src");
        try {
            return findFile(stdLibPath, id, extensions);
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    private static File findFile(@NotNull Path rootPath,
                                 @NotNull String fileNameWithoutExt,
                                 @NotNull List<String> extensions) throws IOException {
        FileLocator l = new FileLocator(fileNameWithoutExt, extensions);
        Files.walkFileTree(rootPath, l);
        if (l.getFile() == null) throw new NoSuchFileException(fileNameWithoutExt);
        return l.getFile();
    }
}