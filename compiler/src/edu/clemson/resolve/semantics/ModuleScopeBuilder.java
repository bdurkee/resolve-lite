package edu.clemson.resolve.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ModuleScopeBuilder extends ScopeBuilder {

    private final List<ModuleIdentifier> importedModules = new ArrayList<>();

    /** The set of all modules {@code this} either extends or inherits from. */
    private final Set<ModuleIdentifier> locallyInheritedModules = new LinkedHashSet<>();

    ModuleScopeBuilder(@NotNull DumbMathClssftnHandler g, @Nullable ModuleIdentifier e,
                       @Nullable ParserRuleContext definingTree,
                       @NotNull Scope parent,
                       @NotNull MathSymbolTable symbolTable) {
        super(symbolTable, g, definingTree, parent, e);
    }

    @NotNull
    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    @NotNull
    public ModuleScopeBuilder addInheritedModules(@NotNull ModuleIdentifier... i) {
        locallyInheritedModules.addAll(Arrays.asList(i));
        return this;
    }

    @NotNull
    public ModuleScopeBuilder addImports(@Nullable Collection<ModuleIdentifier> imports) {
        if (imports != null) {
            importedModules.addAll(imports);
        }
        return this;
    }

    public boolean imports(@Nullable ModuleIdentifier i) {
        return i != null && i.equals(getModuleIdentifier()) || importedModules.contains(i);
    }

    @NotNull
    public List<ModuleIdentifier> getImports() {
        return new ArrayList<>(importedModules);
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
