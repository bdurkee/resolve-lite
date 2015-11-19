package org.rsrg.semantics;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.misc.HardCoded;
import edu.clemson.resolve.parser.ResolveLexer;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class MathSymbolTable {

    private static final Scope DUMMY_RESOLVER = new DummyIdentifierResolver();

    /**
     * When starting a search from a particular scope, specifies how any
     * available facilities should be searched.
     * 
     * Available facilities are those facilities defined in a module searched by
     * the search's {@code ImportStrategy} (which necessarily always includes
     * the source module).
     * 
     * Note that facilities cannot be recursively searched. Imports and
     * facilities appearing in available facilities will not be searched.
     */
    public static enum FacilityStrategy {

        /**
         * Indicates that available facilities should not be searched. The
         * default strategy.
         */
        FACILITY_IGNORE,

        /**
         * Indicates that available facilities should be searched with
         * generic types instantiated. That is, any types used by symbols
         * inside the facility should be updated to reflect the particular
         * instantiation of the generic types.
         */
        FACILITY_INSTANTIATE,

        /**
         * Indicates that available facilities should be searched with
         * generic types intact. That is, any types used by symbols inside the
         * facility will appear exactly as listed in the source file--including
         * references to generics--even if we could use information from the
         * facility to "fill them in."
         */
        FACILITY_GENERIC
    }

    /**
     * When starting a search from a particular scope, specifies which
     * additional modules should be searched, based on any imported modules.
     * 
     * Imported modules are those listed in the <em>uses</em> clause of the
     * source module scope in which the scope is introduced. For searches
     * originating directly in a module scope, the source module scope is the
     * scope itself. In addition to those scopes directly imported in the
     * <em>uses</em> clause, any modules implicitly imported will also be
     * searched. Implicitly imported modules include the standard modules (
     * <code>Std_Boolean_Fac</code>, etc.), and any modules named in the header
     * of the source module (e.g., an enhancement realization implicitly imports
     * it's associate enhancement and concept.)
     */
    public static enum ImportStrategy {

        /**
         * Indicates that imported modules should not be searched. The
         * default strategy.
         */
        IMPORT_NONE {

            public ImportStrategy cascadingStrategy() {
                return IMPORT_NONE;
            }

            public boolean considerImports() {
                return false;
            }
        },

        /**
         * Indicates that only those modules imported directly from the
         * source module should be searched.
         */
        IMPORT_NAMED {

            public ImportStrategy cascadingStrategy() {
                return IMPORT_NONE;
            }

            public boolean considerImports() {
                return true;
            }
        },

        /**
         * Indicates that the search should recursively search the closure of
         * all imports and their own imports.
         */
        IMPORT_RECURSIVE {

            public ImportStrategy cascadingStrategy() {
                return IMPORT_RECURSIVE;
            }

            public boolean considerImports() {
                return true;
            }
        };

        /**
         * Returns the strategy that should be used to recursively search
         * any imported modules.
         * 
         * @return The strategy that should be used to recursively search any
         *         imported modules.
         */
        public abstract ImportStrategy cascadingStrategy();

        /**
         * Returns {@code true} <strong>iff</strong> this strategy
         * requires searching directly imported modules.
         * 
         * @return {@code true} <strong>iff</strong> this strategy
         *         requires searching directly imported modules.
         */
        public abstract boolean considerImports();
    }

    private final Deque<ScopeBuilder> lexicalScopeStack = new LinkedList<>();
    private final Map<String, ModuleScopeBuilder> moduleScopes = new HashMap<>();

    private final ParseTreeProperty<ScopeBuilder> scopes =
            new ParseTreeProperty<>();

    private ModuleScopeBuilder curModuleScope = null;
    private final TypeGraph typeGraph;

    public MathSymbolTable() {
        this.typeGraph = new TypeGraph();

        //The only things in global scope are built-in things
        ScopeBuilder globalScope =
                new ScopeBuilder(this, typeGraph, null, DUMMY_RESOLVER,
                        "GLOBAL");

        HardCoded.addBuiltInSymbols(typeGraph, globalScope);
        lexicalScopeStack.push(globalScope);
    }

    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public ModuleScopeBuilder startModuleScope(AnnotatedModule tree) {

        if (tree == null) {
            throw new IllegalArgumentException("tree may not be null");
        }
        ParseTree contextTree = tree.getRoot();

        if (curModuleScope != null) {
            throw new IllegalStateException("module scope already open");
        }
        ScopeBuilder parent = lexicalScopeStack.peek();
        ModuleScopeBuilder s = new ModuleScopeBuilder(typeGraph,
                tree.getName(), (ParserRuleContext)contextTree, parent, this);
        curModuleScope = s;
        addScope(s, parent);
        moduleScopes.put(s.getModuleID(), s);
        return s;
    }

    public ScopeBuilder startScope(ParserRuleContext definingTree) {
        if ( definingTree == null ) {
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
        if ( lexicalScopeStack.size() == 1 ) {
            result = null;
            curModuleScope = null;
        }
        else {
            result = lexicalScopeStack.peek();
        }
        return result;
    }

    public ScopeBuilder getInnermostActiveScope() {
        checkScopeOpen();
        return lexicalScopeStack.peek();
    }

    private void checkModuleScopeOpen() {
        if ( curModuleScope == null ) {
            throw new IllegalStateException("no open module scope");
        }
    }

    private void checkScopeOpen() {
        if ( lexicalScopeStack.size() == 1 ) {
            throw new IllegalStateException("no open scope");
        }
    }

    private void addScope(ScopeBuilder s, ScopeBuilder parent) {
        parent.addChild(s);
        lexicalScopeStack.push(s);
        scopes.put(s.getDefiningTree(), s);
    }

    public ScopeBuilder getScope(ParserRuleContext e) {
        if (scopes.get(e) == null) {
            throw new IllegalArgumentException("no such scope: " + e.getText());
        }
        return scopes.get(e);
    }

    public ModuleScopeBuilder getModuleScope(@NotNull String name)
            throws NoSuchModuleException {
        return getModuleScope(new CommonToken(ResolveLexer.ID, name));
    }

    public ModuleScopeBuilder getModuleScope(@NotNull Token name)
            throws NoSuchModuleException {
        ModuleScopeBuilder module = moduleScopes.get(name.getText());
        if (module == null) {
            throw new NoSuchModuleException(name);
        }
        return module;
    }
}
