package org.rsrg.semantics;

import edu.clemson.resolve.typereasoning.TypeGraph;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.*;

public class ModuleScopeBuilder extends ScopeBuilder {

    private final List<String> importedModules = new LinkedList<>();

    /**
     * Related modules are those that have an immediate relationship with this
     * one. For instance, if I represent a concept implementation, a related
     * module is my corresponding concept. If I represent a facility, theory,
     * or any other specification for that matter -- I should have no spec
     * module. Note too that all modules are related to themselves, so this
     * will always contain at least one element.
     */
    private Set<String> relatedModules = new HashSet<>();

    ModuleScopeBuilder(TypeGraph g, String name, ParserRuleContext definingTree,
                       Scope parent, SymbolTable symbolTable) {
        super(symbolTable, g, definingTree, parent, name);
        this.relatedModules.add(name);
    }

    public String getModuleID() {
        return moduleID;
    }

    public ModuleScopeBuilder addImports(List<String> importList) {
        importList.stream().filter(u ->
                !importedModules.contains(u) && !moduleID.equals(u))
                .forEach(importedModules::add);
        return this;
    }

    public ModuleScopeBuilder addParentSpecificationRelationship(String e) {
        this.relatedModules.add(e);
        return this;
    }

    public ModuleScopeBuilder addImports(String... i) {
        return addImports(Arrays.asList(i));
    }

    public boolean imports(String i) {
        return i.equals(getModuleID()) || importedModules.contains(i);
    }

    public Set<String> getRelatedModules() {
        return relatedModules;
    }

    public List<String> getImports() {
        return new ArrayList<>(importedModules);
    }

    @Override public String toString() {
        return moduleID + ":" + symbols.keySet();
    }

}
