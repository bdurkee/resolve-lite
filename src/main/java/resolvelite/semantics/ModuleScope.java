package resolvelite.semantics;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import resolvelite.compiler.tree.AnnotatedTree;
import resolvelite.misc.Utils;
import resolvelite.typereasoning.TypeGraph;

import java.util.*;

public class ModuleScope extends BaseScope {
    private final Set<String> importedModules = new LinkedHashSet<>();
    private final AnnotatedTree wrappedTree;

    public ModuleScope(Scope scope, SymbolTable scopeRepo, AnnotatedTree m) {
        super(scope, scopeRepo, m.getName());
        this.wrappedTree = m;
    }

    @Override public String getScopeDescription() {
        return "module";
    }

    public AnnotatedTree getWrappedModuleTree() {
        return wrappedTree;
    }

    public ModuleScope addImports(Set<String> imports) {
        imports.forEach(importedModules::add);
        return this;
    }

    public Set<String> getImports() {
        return importedModules;
    }
}
