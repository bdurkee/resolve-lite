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

    @NotNull
    public static final ModuleIdentifier GLOBAL = new ModuleIdentifier();

    @NotNull
    private Token name;
    private final boolean globalFlag;
    @Nullable private File file;

    @NotNull
    public List<String> pathListRelativeToRoot = new ArrayList<>();

    @NotNull
    public List<Token> fromClausePath = new ArrayList<>();

    private ModuleIdentifier() {
        this.name = new CommonToken(ResolveLexer.ID, "GLOBAL");
        this.globalFlag = true;
    }

    public ModuleIdentifier(@NotNull Token t) {
        this(t, new ArrayList<>());
    }

    public ModuleIdentifier(@NotNull Token t, @Nullable File file) {
        this.name = t;
        this.file = file;
        this.globalFlag = false;
    }

    public ModuleIdentifier(Token t, @NotNull List<Token> fromPath) {
        this.name = t;
        //NoSuchFileException;
        String fromString = Utils.join(fromPath, File.separator);
        //use
        //throw a no such module exception
        //use path searcher to find a file.
        this.fromClausePath.addAll(fromPath);
        this.globalFlag = false;
    }

    @NotNull public File getFile() {
        return null;
    }

    @NotNull
    public String getNameString() {
        return name.getText();
    }

    @NotNull
    public String getFullyQualifiedName() {
        return this.toString();
    }

    @NotNull
    public Token getNameToken() {
        return name;
    }

    @NotNull
    public List<Token> getFromPath() {
        return fromClausePath;
    }

    @Nullable
    public String getQualifiedFromPath() {
        return Utils.join(fromClausePath, ".");
    }

    public static String getModuleFilePathRelativeToProjectLibDirs(String filePath) {
        String resolveRoot = RESOLVECompiler.getCoreLibraryDirectory() + File.separator + "src";
        String resolvePath = RESOLVECompiler.getLibrariesPathDirectory() + File.separator + "src";

        String result = null;
        Path modulePath = new File(filePath).toPath();
        if (modulePath.startsWith(resolvePath)) {
            Path projectPathAbsolute = Paths.get(new File(resolvePath).getAbsolutePath());
            Path pathRelative = projectPathAbsolute.relativize(modulePath);
            result = pathRelative.toString();
        }
        else if (modulePath.startsWith(resolveRoot)) {
            Path projectPathAbsolute = Paths.get(new File(resolveRoot).getAbsolutePath());
            Path pathRelative = projectPathAbsolute.relativize(modulePath);
            result = pathRelative.toString();
        }
        else {
            //just use the lib directory if the user has a non-conformal project..
            result = new File(modulePath.toFile().getPath()).getPath();
        }
        return result;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        boolean result = (o instanceof ModuleIdentifier);
        if (result) {
            result = ((ModuleIdentifier) o).name.getText().equals(name.getText());
        }
        return result;
    }

    public int hashCode() {
        return name.getText().hashCode();
    }

    @Override
    public int compareTo(@NotNull ModuleIdentifier o) {
        return name.getText().compareTo(o.name.getText());
    }

    @NotNull
    public String toString() {
        return name.getText();
    }
}
