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

    public UsesListener(@NotNull RESOLVECompiler rc) {
        this.compiler = rc;
    }

    @Override
    public void exitUsesList(ResolveParser.UsesListContext ctx) {
        for (ResolveParser.UsesSpecContext u : ctx.usesSpec()) {
            File f = resolveImport(compiler, u);
            if (f == null) {
                compiler.errMgr.semanticError(ErrorKind.MISSING_IMPORT_FILE, u.ID().getSymbol(), u.ID().getText());
                continue;
            }
            uses.add(new ModuleIdentifier(u.ID().getSymbol(), f));
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
        ResolveParser.QualifiedFromPathContext specFrom = ctx.specFrom != null ?
                ctx.specFrom.qualifiedFromPath() : null;
        ResolveParser.QualifiedFromPathContext implFrom = ctx.implFrom != null ?
                ctx.implFrom.qualifiedFromPath() : null;

        resolveAndAddFacilitySpecOrImpl(ctx.spec, false, specFrom);
        resolveAndAddFacilitySpecOrImpl(ctx.impl, ctx.externally != null, implFrom);
    }

    private void resolveAndAddFacilitySpecOrImpl(@NotNull Token t,
                                                 boolean isExternal,
                                                 @Nullable ResolveParser.QualifiedFromPathContext from) {
        if (!isExternal) {
            //we're not an external implementation
            File resolve = resolveImport(compiler, t, from);

            if (resolve != null) {
                uses.add(new ModuleIdentifier(t, resolve));
            }
            else {
                compiler.errMgr.semanticError(ErrorKind.MISSING_IMPORT_FILE, t, t.getText());
            }
        }
        else {
            //we're an external implementation..
            File resolveExternal = resolveImport(compiler, t, from, RESOLVECompiler.NON_NATIVE_FILE_EXTENSION);
            if (resolveExternal != null) {
                extUses.add(new ModuleIdentifier(t, resolveExternal));
            }
            else {
                compiler.errMgr.semanticError(ErrorKind.MISSING_IMPORT_FILE, t, t.getText());
            }
        }
    }

/*  @Override
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

    //so we don't have Path resolveProjRootPath here because our projects should be on RESOLVEPATH anyways..
    @Nullable
    private static Path getAppropriateRootDirectoryForFromClause(@NotNull Token t, @NotNull String fromStem) {
        Path resolveStdRootPath = Paths.get(RESOLVECompiler.getCoreLibraryDirectory() + File.separator + "src");
        Path resolveLibRootPath = Paths.get(RESOLVECompiler.getLibrariesPathDirectory() + File.separator + "src");

        File result = new File(resolveStdRootPath.toString(), fromStem);
        if (result.exists() && result.isDirectory()) return result.toPath();
        result = new File(resolveLibRootPath.toString(), fromStem);
        if (result.exists() && result.isDirectory()) return result.toPath();
        return null;
    }

    @Nullable
    public static File resolveImport(@NotNull RESOLVECompiler compiler,
                                     @NotNull ResolveParser.UsesSpecContext u) {
        return resolveImport(compiler, u.ID().getSymbol(), u.fromClauseSpec() != null ?
                u.fromClauseSpec().qualifiedFromPath() : null, RESOLVECompiler.NATIVE_FILE_EXTENSION);
    }

    @Nullable
    public static File resolveImport(@NotNull RESOLVECompiler compiler,
                                     @NotNull Token usesToken,
                                     @Nullable ResolveParser.QualifiedFromPathContext fromPathCtx,
                                     @NotNull String ... extensions) {
        List<String> exts = (extensions.length == 0) ?
                Collections.singletonList(RESOLVECompiler.NATIVE_FILE_EXTENSION) :
                Arrays.asList(extensions);
        return resolveImport(compiler, usesToken, fromPathCtx, exts);
    }

    @Nullable
    public static File resolveImport(@NotNull RESOLVECompiler compiler,
                                     @NotNull Token usesToken,
                                     @Nullable ResolveParser.QualifiedFromPathContext fromPathCtx,
                                     @NotNull List<String> extensions) {
        //first check to see if we're on RESOLVEPATH
        Path projectPath = Paths.get(compiler.libDirectory).toAbsolutePath();
        Path resolvePath = Paths.get(RESOLVECompiler.getLibrariesPathDirectory()).toAbsolutePath();
        File result = null;
        if (fromPathCtx != null) {
            //a fromclause can either describe something on RESOLVEROOT or it can describe the root
            //of some other resolve project on RESOLVEPATH

            Path s = getAppropriateRootDirectoryForFromClause(usesToken,
                    fromPathCtx.getText().replace('.', File.separatorChar));
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
            result = searchProjectRootDirectory(extensions, compiler, usesToken.getText());

            //now search the
            //then search the std libs.. if we didn't find anything
            if (result == null) result = searchStdRootDirectory(extensions, usesToken.getText());
        }
        return result;
    }

    @Nullable
    private static File searchProjectRootDirectory(List<String> extensions, RESOLVECompiler compiler, String id) {
        Path projectPath = Paths.get(compiler.libDirectory).toAbsolutePath();
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