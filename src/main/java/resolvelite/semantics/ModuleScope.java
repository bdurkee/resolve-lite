package resolvelite.semantics;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import resolvelite.misc.Utils;
import resolvelite.typereasoning.TypeGraph;

import java.util.*;

public class ModuleScope extends BaseScope {
    private final Set<String> importedModules = new LinkedHashSet<>();

    public ModuleScope(Scope scope, SymbolTable scopeRepo) {
        super(scope, scopeRepo);
    }

    @Override public String getScopeDescription() {
        return "module";
    }

    public ModuleScope addImports(Set<Token> imports) {
        imports.forEach(i -> importedModules.add(i.getText()));
        return this;
    }

    @Override public Set<String> getImports() {
        return importedModules;
    }
}
