package edu.clemson.resolve.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ModuleScopeBuilder extends ScopeBuilder {

    private final Set<ModuleIdentifier> importedModules = new HashSet<>();

    /** The set of all modules {@code this} either extends or inherits from. */
    private final Set<ModuleIdentifier> locallyInheritedModules = new LinkedHashSet<>();

    ModuleScopeBuilder(@NotNull DumbMathClssftnHandler g, @NotNull ModuleIdentifier e,
                       @Nullable ParserRuleContext definingTree,
                       @NotNull Scope parent,
                       @NotNull MathSymbolTable symbolTable) {
        super(symbolTable, g, definingTree, parent, e);
    }

    @NotNull
    public ModuleScopeBuilder addInheritedModules(@NotNull ModuleIdentifier... i) {
        locallyInheritedModules.addAll(Arrays.asList(i));
        return this;
    }

    @NotNull
    public ModuleScopeBuilder addImports(@NotNull Collection<ModuleIdentifier> imports) {
        importedModules.addAll(imports);
        return this;
    }

    public boolean imports(@Nullable ModuleIdentifier i) {
        return i != null && i.equals(getModuleIdentifier()) || importedModules.contains(i);
    }

    //Implicitly referenced imports are included in this set as well. See {@link UsesListener}.
    @NotNull
    public Set<ModuleIdentifier> getImports() {
        return new HashSet<>(importedModules);
    }

    @Nullable
    public ModuleIdentifier getImportWithName(@NotNull Token name) {
        for (ModuleIdentifier e : importedModules) {
            if (e.getNameToken().getText().equals(name.getText())) {
                return e;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public String toString() {
        return moduleIdentifier + ":" + symbols.keySet();
    }

    public Set<ModuleIdentifier> getInheritedIdentifiers() {
        return new LinkedHashSet<>(locallyInheritedModules);
    }
}
