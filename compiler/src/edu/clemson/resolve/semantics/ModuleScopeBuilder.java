package edu.clemson.resolve.semantics;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ModuleScopeBuilder extends ScopeBuilder {

    private final Set<ModuleIdentifier> importedModules = new HashSet<>();
    private final Set<ModuleIdentifier> facilityModules = new HashSet<>();

    /**
     * The set of all modules {@code this} either extends or inherits from. This set should be a subset of
     * {@link #importedModules}.
     */
    private final Set<ModuleIdentifier> inheritedModules = new LinkedHashSet<>();
    private final Map<String, ModuleIdentifier> aliases = new HashMap<>();

    ModuleScopeBuilder(@NotNull DumbMathClssftnHandler g,
                       @NotNull ModuleIdentifier e,
                       @Nullable ParserRuleContext definingTree,
                       @NotNull Scope parent,
                       @NotNull MathSymbolTable symbolTable) {
        super(symbolTable, g, definingTree, parent, e);
    }

    @NotNull
    public ModuleScopeBuilder addAliases(@NotNull Map<String, ModuleIdentifier> aliases) {
        this.aliases.putAll(aliases);
        return this;
    }

    @NotNull
    public ModuleScopeBuilder addInheritedModules(@NotNull ModuleIdentifier... i) {
        inheritedModules.addAll(Arrays.asList(i));
        return this;
    }

    @NotNull
    public ModuleScopeBuilder addImports(@NotNull Collection<ModuleIdentifier> imports) {
        importedModules.addAll(imports);
        return this;
    }

    @NotNull
    public ModuleScopeBuilder addFacilityImports(@NotNull Collection<ModuleIdentifier> facilityIdentifiers) {
        facilityModules.addAll(facilityIdentifiers);
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

    //TODO: Use a map instead... but need to
    @NotNull
    public ModuleIdentifier getImportWithName(@NotNull Token name) throws NoSuchModuleException {
        for (ModuleIdentifier e : importedModules) {
            if (e.getNameToken().getText().equals(name.getText())) {
                return e;
            }
        }
        throw new NoSuchModuleException(name);
    }

    //TODO: Use a map instead..
    @NotNull
    public ModuleIdentifier getFacilityImportWithName(@NotNull Token name) throws NoSuchModuleException {
        for (ModuleIdentifier e : facilityModules) {
            if (e.getNameToken().getText().equals(name.getText())) {
                return e;
            }
        }
        throw new NoSuchModuleException(name);
    }

    @NotNull
    public ModuleIdentifier getAlias(@NotNull Token name) throws NoSuchModuleException {
        if (aliases.get(name.getText()) == null) {
            throw new NoSuchModuleException(name);
        }
        return aliases.get(name.getText());
    }

    @NotNull
    @Override
    public String toString() {
        return moduleIdentifier + ":" + symbols.keySet();
    }

    public Set<ModuleIdentifier> getInheritedIdentifiers() {
        return new LinkedHashSet<>(inheritedModules);
    }

    public Map<String, ModuleIdentifier> getAliases() {
        return aliases;
    }
}
