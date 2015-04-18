package org.resolvelite.semantics;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SymbolTable {

    private final Deque<ScopeBuilder> lexicalScopeStack = new LinkedList<>();

    public final ParseTreeProperty<ScopeBuilder> scopes =
            new ParseTreeProperty<>();
    public final Map<String, ModuleScopeBuilder> myModuleScopes =
            new HashMap<>();

    private ModuleScopeBuilder curModuleScope = null;

    private final TypeGraph typeGraph;

    public SymbolTable() {
        this.typeGraph = new TypeGraph();

        //The only things in global scope are built-in things
        ScopeBuilder globalScope =
                new ScopeBuilder(this, typeGraph, null, null, "GLOBAL");

        //HardCoded.addBuiltInSymbols(typeGraph, globalScope);
        lexicalScopeStack.push(globalScope);
    }

    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public ModuleScopeBuilder startModuleScope(ParseTree module, String name) {

        if (module == null) {
            throw new IllegalArgumentException("module may not be null");
        }
        if (curModuleScope != null) {
            throw new IllegalStateException("module scope already open");
        }
        ScopeBuilder parent = lexicalScopeStack.peek();
        ModuleScopeBuilder s =
                new ModuleScopeBuilder(typeGraph, name, module, parent, this);
        curModuleScope = s;
        addScope(s, parent);
        myModuleScopes.put(s.getModuleID(), s);
        return s;
    }

    public ScopeBuilder startScope(ParseTree definingTree) {
        if (definingTree == null) {
            throw new IllegalArgumentException("defining tree may not be null");
        }
        checkModuleScopeOpen();
        ScopeBuilder parent = lexicalScopeStack.peek();
        ScopeBuilder s =
                new ScopeBuilder(this, typeGraph, definingTree, parent,
                        curModuleScope.getModuleID());

        addScope(s, parent);
        return s;
    }

    public ScopeBuilder endScope() {
        checkScopeOpen();
        lexicalScopeStack.pop();
        ScopeBuilder result;
        if (lexicalScopeStack.size() == 1) {
            result = null;
            curModuleScope = null;
        }
        else {
            result = lexicalScopeStack.peek();
        }
        return result;
    }

    private void checkModuleScopeOpen() {
        if (curModuleScope == null) {
            throw new IllegalStateException("no open module scope");
        }
    }

    private void checkScopeOpen() {
        if (lexicalScopeStack.size() == 1) {
            throw new IllegalStateException("no open scope");
        }
    }

    private void addScope(ScopeBuilder s, ScopeBuilder parent) {
        lexicalScopeStack.push(s);
        scopes.put(s.getDefiningTree(), s);
    }
}
