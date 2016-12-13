package edu.clemson.resolve.compiler;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.misc.FileLocator;
import edu.clemson.resolve.misc.Utils;
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
public class DependencyCollectingListener extends ResolveBaseListener {

    private final RESOLVECompiler compiler;

    private final String fileName;
    private final DependencyHolderBuilder tracker = new DependencyHolderBuilder();

    public DependencyCollectingListener(@NotNull String fileName, @NotNull RESOLVECompiler rc) {
        this.compiler = rc;
        this.fileName = fileName;
    }

    @NotNull
    public DependencyHolder getDependencies() {
        return tracker.build();
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
            tracker.addUsesItem(e);
        }
    }

    @Override
    public void exitFacilityDecl(ResolveParser.FacilityDeclContext ctx) {
        ResolveParser.ModuleLibraryIdentifierContext specFrom = ctx.specFrom != null ?
                ctx.specFrom.moduleLibraryIdentifier() : null;
        ResolveParser.ModuleLibraryIdentifierContext implFrom = ctx.realizFrom != null ?
                ctx.realizFrom.moduleLibraryIdentifier() : null;

        resolveAndAddFacilitySpecOrImpl(ctx.spec, false, specFrom);
        resolveAndAddFacilitySpecOrImpl(ctx.realiz, ctx.externally != null, implFrom);
    }

    /*
    @Override
    public void exitEnhancementPairing(ResolveParser.EnhancementPairingContext ctx) {
        ResolveParser.ModuleLibraryIdentifierContext specFrom = ctx.specFrom != null ?
                ctx.specFrom.moduleLibraryIdentifier() : null;
        ResolveParser.ModuleLibraryIdentifierContext implFrom = ctx.realizFrom != null ?
                ctx.realizFrom.moduleLibraryIdentifier() : null;

        resolveAndAddFacilitySpecOrImpl(ctx.spec, false, specFrom);
        resolveAndAddFacilitySpecOrImpl(ctx.realiz, ctx.externally != null, implFrom);
    }*/

    private void resolveAndAddFacilitySpecOrImpl(@NotNull Token t,
                                                 boolean isExternal,
                                                 @Nullable ResolveParser.ModuleLibraryIdentifierContext from) {
        if (!isExternal) {
            //we're not an external implementation
            File resolve = resolveImport(t, from);

            if (resolve != null) {
                tracker.addFacilityUsesItem(new ModuleIdentifier(t, resolve));
            }
            else {
                compiler.errMgr.semanticError(ErrorKind.MISSING_IMPORT_FILE, t, t.getText());
            }
        }
        else {
            //we're an external implementation..
            File resolveExternal = resolveImport(t, from, RESOLVECompiler.NON_NATIVE_FILE_EXTENSION);
            if (resolveExternal != null) {
                tracker.addExternalUsesItem(new ModuleIdentifier(t, resolveExternal));
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
            //search the current project directory for usesToken..
            result = searchProjectRootDirectory(extensions, usesToken.getText());

            //if not found, then search the std libs...
            if (result == null) result = searchStdRootDirectory(extensions, usesToken.getText());
        }
        return result;
    }

    @Nullable
    private File searchProjectRootDirectory(List<String> extensions, String id) {
        Path projectPath = RESOLVECompiler.getProjectRootPathFor(fileName);
        try {
            return findFile(projectPath, id, extensions);
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    private File searchStdRootDirectory(List<String> extensions, String id) {
        Path stdLibPath = Paths.get(RESOLVECompiler.getCoreLibraryDirectory() + File.separator + "src");
        try {
            return findFile(stdLibPath, id, extensions);
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    private File findFile(@NotNull Path rootPath,
                          @NotNull String fileNameWithoutExt,
                          @NotNull List<String> extensions) throws IOException {
        FileLocator l = new FileLocator(fileNameWithoutExt, extensions);
        Files.walkFileTree(rootPath, l);
        if (l.getFile() == null) throw new NoSuchFileException(fileNameWithoutExt);
        File result = l.getFile();
        return result;
    }

    public static class DependencyHolder {
        public final Set<ModuleIdentifier> uses = new HashSet<>();
        public final Set<ModuleIdentifier> facilityUses = new HashSet<>();
        public final Set<ModuleIdentifier> externalUses = new HashSet<>();

        private DependencyHolder(DependencyHolderBuilder builder) {
            this.uses.addAll(builder.uses);
            this.externalUses.addAll(builder.externalUses);
            this.facilityUses.addAll(builder.facilityUses);
        }

        /** Returns the set of uses items + those from facilities (excludes external uses) */
        @NotNull
        public Set<ModuleIdentifier> getCombinedUses() {
            Set<ModuleIdentifier> result = new HashSet<>(facilityUses);
            result.addAll(uses);
            return result;
        }
    }

    public static class DependencyHolderBuilder implements Utils.Builder<DependencyHolder> {
        protected final Set<ModuleIdentifier> externalUses = new HashSet<>();
        protected final Set<ModuleIdentifier> facilityUses = new HashSet<>();
        protected final Set<ModuleIdentifier> uses;

        public DependencyHolderBuilder() {
            this(new HashSet<>());
        }

        public DependencyHolderBuilder(@NotNull Collection<ModuleIdentifier> initialIdentifiers) {
            this.uses = new HashSet<>(initialIdentifiers);
        }

        public DependencyHolderBuilder addUsesItem(@NotNull ModuleIdentifier identifier) {
            uses.add(identifier);
            return this;
        }

        public DependencyHolderBuilder addUsesItems(@NotNull Collection<ModuleIdentifier> identifiers) {
            for (ModuleIdentifier identifier : identifiers) {
                uses.add(identifier);
            }
            return this;
        }

        public DependencyHolderBuilder addExternalUsesItem(@NotNull ModuleIdentifier identifier) {
            externalUses.add(identifier);
            return this;
        }

        public DependencyHolderBuilder addFacilityUsesItem(@NotNull ModuleIdentifier identifier) {
            facilityUses.add(identifier);
            return this;
        }

        @NotNull
        @Override
        public DependencyHolder build() {
            return new DependencyHolder(this);
        }
    }


}