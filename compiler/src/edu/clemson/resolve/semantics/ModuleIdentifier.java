package edu.clemson.resolve.semantics;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.misc.FileLocator;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Identifies a particular module unambiguously.
 * <p>
 * <strong>Note:</strong> Currently, we only permit one level of namespace. But ultimately that will probably change
 * (because, for example, at this moment if there were two "Stack_Templates", we couldn't deal with that. A
 * java class-path-like solution seems inevitable.  For the moment however, this is just a wrapper around the token
 * based name of the module to facilitate changing how we deal with modules later.</p>
 * <p>
 * We use {@link Token}s internally here so we have position information for the module we're identifying is readily
 * available (mostly for error rendering purposes).</p>
 */
public class ModuleIdentifier implements Comparable<ModuleIdentifier> {

    public static final ModuleIdentifier GLOBAL = new ModuleIdentifier();

    private final Token name;
    private final boolean globalFlag;
    private final File file;

    private ModuleIdentifier() {
        this.name = new CommonToken(ResolveLexer.ID, "GLOBAL");
        this.globalFlag = true;
        file = new File(".");
    }

    public ModuleIdentifier(@NotNull Token t, @NotNull File file) {
        this.name = t;
        this.file = file;
        this.globalFlag = false;
    }

    @NotNull
    public File getFile() {
        return file;
    }

    @NotNull
    public String getNameString() {
        return name.getText();
    }

    @NotNull
    public Token getNameToken() {
        return name;
    }

    @NotNull
    public String getPackageRoot() {
        return getPackageRootPath().toString();
    }

    @NotNull
    private Path getPackageRootPath() {
        Path libraryPath = Paths.get(RESOLVECompiler.getLibrariesPathDirectory() + File.separator + "src");
        Path stdlibPath = Paths.get(RESOLVECompiler.getCoreLibraryDirectory() + File.separator + "src");

        Path filePath = Paths.get(file.getAbsolutePath());
        return filePath.startsWith(libraryPath) ? libraryPath : stdlibPath;
    }

    @NotNull
    public Path getPathRelativeToRootDir() {
        Path filePath = Paths.get(file.getAbsolutePath());
        return getPackageRootPath().relativize(filePath);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        boolean result = (o instanceof ModuleIdentifier);
        if (result) {
            result = ((ModuleIdentifier) o).name.getText().equals(name.getText()) &&
                    ((ModuleIdentifier) o).getFile().getAbsolutePath().equals(file.getAbsolutePath());
        }
        return result;
    }

    @Override
    public int hashCode() {
        return name.getText().hashCode();
    }

    @Override
    public int compareTo(@NotNull ModuleIdentifier o) {
        return name.getText().compareTo(o.name.getText());
    }

    @NotNull
    public String toString() {
        String fromPathStr = getPathRelativeToRootDir().getParent().toString();
        return name.getText() + " from " + fromPathStr.replaceAll(File.separator, ".");
    }

    public static ModuleIdentifier createInvalidModuleIdentifier(Token name) {
        return new ModuleIdentifier(name, new File("."));
    }
}
