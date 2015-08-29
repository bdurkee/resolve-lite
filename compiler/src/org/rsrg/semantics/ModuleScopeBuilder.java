package org.rsrg.semantics;

import edu.clemson.resolve.compiler.AnnotatedTree;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;

public class ModuleScopeBuilder extends ScopeBuilder {

    private final List<String> importedModules = new LinkedList<>();
    private Set<String> dependentTerms = new HashSet<>();

    ModuleScopeBuilder(TypeGraph g, String name, ParserRuleContext definingTree,
                       Scope parent, SymbolTable symbolTable) {
        super(symbolTable, g, definingTree, parent, name);
    }

    public String getModuleID() {
        return moduleID;
    }

    public ModuleScopeBuilder addImports(Collection<String> imports) {
        importedModules.addAll(imports);
        return this;
    }

    public boolean imports(String i) {
        return i.equals(getModuleID()) || importedModules.contains(i);
    }

    public ModuleScopeBuilder addDependentTerms(Collection<String> terms) {
        dependentTerms.addAll(terms);
        return this;
    }

    public Set<String> getDependentTerms() {
        return dependentTerms;
    }

    public List<String> getImports() {
        return new ArrayList<>(importedModules);
    }

    @Override public String toString() {
        return moduleID+":"+symbols.keySet();
    }

}
