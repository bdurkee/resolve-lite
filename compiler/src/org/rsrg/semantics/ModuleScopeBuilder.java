package org.rsrg.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ModuleScopeBuilder extends ScopeBuilder {

    @NotNull private final List<ModuleIdentifier> importedModules =
            new ArrayList<>();

    ModuleScopeBuilder(@NotNull TypeGraph g, @NotNull Token name,
                       @Nullable ParserRuleContext definingTree,
                       @NotNull Scope parent,
                       @NotNull MathSymbolTable symbolTable) {
        super(symbolTable, g, definingTree, parent, new ModuleIdentifier(name));
    }

    @NotNull public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    @NotNull public ModuleScopeBuilder addImports(
            @Nullable Collection<ModuleIdentifier> imports) {
        if (imports != null) {
            importedModules.addAll(imports);
        }
        return this;
    }

    public boolean imports(@Nullable ModuleIdentifier i) {
        return i != null && i.equals(getModuleIdentifier()) ||
                importedModules.contains(i);
    }

    @NotNull public List<ModuleIdentifier> getImports() {
        return new ArrayList<>(importedModules);
    }

    @NotNull @Override public String toString() {
        return moduleIdentifier + ":" + symbols.keySet();
    }

}
