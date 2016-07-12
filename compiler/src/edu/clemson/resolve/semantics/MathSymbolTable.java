package edu.clemson.resolve.semantics;

import edu.clemson.resolve.compiler.AnnotatedModule;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.semantics.query.MultimatchSymbolQuery;
import edu.clemson.resolve.semantics.query.SymbolQuery;
import edu.clemson.resolve.semantics.searchers.TableSearcher;
import edu.clemson.resolve.semantics.symbol.FacilitySymbol;
import edu.clemson.resolve.semantics.symbol.MathClssftnWrappingSymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.*;

public class MathSymbolTable {

    private static final Scope DUMMY_RESOLVER = new DummyIdentifierResolver();

    /**
     * When starting a search from a particular scope, specifies how any available facilities should be searched.
     * <p>
     * Available facilities are those facilities defined in a module searched by the search's {@link ImportStrategy}
     * (which necessarily always includes the source module). Note that facilities cannot be recursively searched.
     * Imports and facilities appearing in available facilities will not be searched.
     */
    public static enum FacilityStrategy {

        /** Indicates that available facilities should not be searched. The default strategy. */
        FACILITY_IGNORE,

        /**
         * Indicates that available facilities should be searched with generic types instantiated. That is, any types
         * used by symbols inside the facility should be updated to reflect the particular instantiation of the
         * generic types.
         */
        FACILITY_INSTANTIATE,

        /**
         * Indicates that available facilities should be searched with generic types intact. That is, any types used
         * by symbols inside the facility will appear exactly as listed in the source file--including references to
         * generics--even if we could use information from the facility to "fill them in."
         */
        FACILITY_GENERIC
    }

    /**
     * When starting a search from a particular scope, specifies which additional modules should be searched, based on
     * any imported modules.
     * <p>
     * Imported modules are those listed in the {@code uses} clause of the source module scope in which the scope is
     * introduced. For searches originating directly in a module scope, the source module scope is the scope itself.
     * In addition to those scopes directly imported in the <em>uses</em> clause, any modules implicitly imported will
     * also be searched. Implicitly imported modules include the standard modules ({@code Std_Bools}, etc.), and any
     * modules named in the header of the source module (e.g., an enhancement realization implicitly imports it's
     * associate enhancement and concept.)
     */
    public static enum ImportStrategy {

        /** Indicates that imported modules should not be searched. The default strategy. */
        IMPORT_NONE {
            public ImportStrategy cascadingStrategy() {
                return IMPORT_NONE;
            }

            public boolean considerImports() {
                return false;
            }
        },

        /** Indicates that only those modules imported directly from the source module should be searched. */
        IMPORT_NAMED {
            public ImportStrategy cascadingStrategy() {
                return IMPORT_NONE;
            }

            public boolean considerImports() {
                return true;
            }
        },

        /** Indicates that the search should recursively search the closure of all imports and their own imports. */
        IMPORT_RECURSIVE {
            public ImportStrategy cascadingStrategy() {
                return IMPORT_RECURSIVE;
            }

            public boolean considerImports() {
                return true;
            }
        };

        /**
         * Returns the strategy that should be used to recursively search any imported modules.
         *
         * @return how we're going to search any imported modules (the strategy)
         */
        public abstract ImportStrategy cascadingStrategy();

        /**
         * Returns {@code true} <strong>iff</strong> this strategy requires searching directly imported modules.
         *
         * @return whether or not we'll search imported modules
         */
        public abstract boolean considerImports();
    }

    @NotNull
    private final Deque<ScopeBuilder> lexicalScopeStack = new LinkedList<>();
    @NotNull
    private final Map<ModuleIdentifier, ModuleScopeBuilder> moduleScopes = new HashMap<>();
    @NotNull
    private final ParseTreeProperty<ScopeBuilder> scopes = new ParseTreeProperty<>();
    @Nullable
    private ModuleScopeBuilder curModuleScope = null;
    @NotNull
    private final DumbMathClssftnHandler typeGraph;

    public MathSymbolTable() {
        this.typeGraph = new DumbMathClssftnHandler();

        //The only things in global scope are built-in things
        ScopeBuilder globalScope = new ScopeBuilder(this, typeGraph, null, DUMMY_RESOLVER, ModuleIdentifier.GLOBAL);
        initializeMathTypeSystem(typeGraph, globalScope);
        lexicalScopeStack.push(globalScope);
    }

    private void initializeMathTypeSystem(@NotNull DumbMathClssftnHandler g, @NotNull ScopeBuilder globalScope) {
        try {
            globalScope.define(new MathClssftnWrappingSymbol(g, "B", g.BOOLEAN));
            globalScope.define(new MathClssftnWrappingSymbol(g, "SSet", g.SSET));
            globalScope.define(new MathClssftnWrappingSymbol(g, "Cls", g.CLS));
            globalScope.define(new MathClssftnWrappingSymbol(g, "El", g.EL));

            globalScope.define(new MathClssftnWrappingSymbol(g, "Empty_Set", g.EMPTY_SET));

            globalScope.define(new MathClssftnWrappingSymbol(g, "and", g.BOOLEAN_FUNCTION));
            globalScope.define(new MathClssftnWrappingSymbol(g, "∧", g.BOOLEAN_FUNCTION));

            globalScope.define(new MathClssftnWrappingSymbol(g, "or", g.BOOLEAN_FUNCTION));
            globalScope.define(new MathClssftnWrappingSymbol(g, "∨", g.BOOLEAN_FUNCTION));

            globalScope.define(new MathClssftnWrappingSymbol(g, "implies", g.BOOLEAN_FUNCTION));
            globalScope.define(new MathClssftnWrappingSymbol(g, "Powerset", g.POWERSET_FUNCTION));
            globalScope.define(new MathClssftnWrappingSymbol(g, "conc", g.BOOLEAN));

            globalScope.define(new MathClssftnWrappingSymbol(g, "is_in",
                    new MathFunctionClssftn(g, g.BOOLEAN, g.ENTITY, g.SSET)));
            globalScope.define(new MathClssftnWrappingSymbol(g, "∈",
                    new MathFunctionClssftn(g, g.BOOLEAN, g.ENTITY, g.SSET)));

            globalScope.define(new MathClssftnWrappingSymbol(g, "is_not_in",
                    new MathFunctionClssftn(g, g.BOOLEAN, g.ENTITY, g.SSET)));
            globalScope.define(new MathClssftnWrappingSymbol(g, "∉",
                    new MathFunctionClssftn(g, g.BOOLEAN, g.ENTITY, g.SSET)));

            globalScope.define(new MathClssftnWrappingSymbol(g, "~",
                    new MathFunctionClssftn(g, g.SSET, g.SSET, g.SSET)));

            globalScope.define(new MathClssftnWrappingSymbol(g, "not", new MathFunctionClssftn(g, g.BOOLEAN, g.BOOLEAN)));
            globalScope.define(new MathClssftnWrappingSymbol(g, "⌐", new MathFunctionClssftn(g, g.BOOLEAN, g.BOOLEAN)));

            globalScope.define(new MathClssftnWrappingSymbol(g, "true", new MathNamedClssftn(g, "true", 0, g.BOOLEAN)));
            globalScope.define(new MathClssftnWrappingSymbol(g, "false", new MathNamedClssftn(g, "false", 0, g.BOOLEAN)));

            //aliases for our 'arrow type'
            globalScope.define(new MathClssftnWrappingSymbol(g, "⟶", g.ARROW_FUNCTION));
            globalScope.define(new MathClssftnWrappingSymbol(g, "->", g.ARROW_FUNCTION));

            globalScope.define(new MathClssftnWrappingSymbol(g, "*", g.CROSS_PROD_FUNCTION));
            globalScope.define(new MathClssftnWrappingSymbol(g, "=",
                    new MathFunctionClssftn(g, g.BOOLEAN, g.ENTITY, g.ENTITY)));
            globalScope.define(new MathClssftnWrappingSymbol(g, "/=",
                    new MathFunctionClssftn(g, g.BOOLEAN, g.ENTITY, g.ENTITY)));
            globalScope.define(new MathClssftnWrappingSymbol(g, "≠",
                    new MathFunctionClssftn(g, g.BOOLEAN, g.ENTITY, g.ENTITY)));
        } catch (DuplicateSymbolException e) {
            throw new RuntimeException("duplicate builtin symbol");
        }
    }

    @NotNull
    public DumbMathClssftnHandler getTypeGraph() {
        return typeGraph;
    }

    @NotNull
    public ModuleScopeBuilder startModuleScope(@NotNull AnnotatedModule module) {
        if (curModuleScope != null) {
            throw new IllegalStateException("module scope already open");
        }
        ParseTree contextTree = module.getRoot();

        ScopeBuilder parent = lexicalScopeStack.peek();
        ModuleScopeBuilder s = new ModuleScopeBuilder(typeGraph, module.getModuleIdentifier(),
                (ParserRuleContext) contextTree, parent, this);
        curModuleScope = s;
        addScope(s, parent);
        moduleScopes.put(s.getModuleIdentifier(), s);
        return s;
    }

    @NotNull
    public ScopeBuilder startScope(@NotNull ParserRuleContext definingTree) {
        if (curModuleScope == null) {
            throw new IllegalStateException("no open module scope");
        }
        ScopeBuilder parent = lexicalScopeStack.peek();
        ScopeBuilder s = new ScopeBuilder(this, typeGraph, definingTree, parent, curModuleScope.getModuleIdentifier());
        addScope(s, parent);
        return s;
    }

    public void addTag(ModuleIdentifier e) {
        checkScopeOpen();
        moduleScopes.put(e, curModuleScope);
    }

    /**
     * Closes the most recently opened, unclosed working scope, including those opened with
     * {@link #startModuleScope(AnnotatedModule)}.
     *
     * @return The new innermost active scope after the former one was closed by this call. If the scope that was
     * closed was the module scope, then returns {@code null}
     */
    @Nullable
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

    public ScopeBuilder getInnermostActiveScope() {
        checkScopeOpen();
        return lexicalScopeStack.peek();
    }

    private void checkScopeOpen() {
        if (lexicalScopeStack.size() == 1) {
            throw new IllegalStateException("no open scope");
        }
    }

    private void addScope(@NotNull ScopeBuilder s, @NotNull ScopeBuilder parent) {
        lexicalScopeStack.push(s);
        scopes.put(s.getDefiningTree(), s);
    }

    @NotNull
    public ScopeBuilder getScope(@NotNull ParserRuleContext e) {
        if (scopes.get(e) == null) {
            throw new IllegalArgumentException("no such scope: " + e.getText());
        }
        return scopes.get(e);
    }

    @NotNull
    public ModuleScopeBuilder getModuleScope(@NotNull ModuleIdentifier identifier) throws NoSuchModuleException {
        ModuleScopeBuilder module = moduleScopes.get(identifier);
        if (module == null) {
            throw new NoSuchModuleException(identifier);
        }
        return module;
    }

    protected static class DummyIdentifierResolver extends AbstractScope {

        @NotNull
        @Override
        public ModuleIdentifier getModuleIdentifier() {
            return ModuleIdentifier.GLOBAL;
        }

        @Override
        @NotNull
        public <E extends Symbol> List<E> query(@NotNull MultimatchSymbolQuery<E> query) {
            return new ArrayList<>();
        }

        @Override
        @NotNull
        public <E extends Symbol> E queryForOne(@NotNull SymbolQuery<E> query)
                throws NoSuchSymbolException, DuplicateSymbolException {
            throw new NoSuchSymbolException();
        }

        @Override
        public <E extends Symbol> boolean addMatches(@NotNull TableSearcher<E> searcher,
                                                     @NotNull List<E> matches,
                                                     @NotNull Set<Scope> searchedScopes,
                                                     @NotNull Map<String, ProgType> genericInstantiations,
                                                     FacilitySymbol instantiatingFacility,
                                                     @NotNull TableSearcher.SearchContext l)
                throws DuplicateSymbolException {
            return false;
        }

        @Override
        @NotNull
        public Symbol define(@NotNull Symbol s) throws DuplicateSymbolException {
            return s;
        }

        @Override
        @NotNull
        public <T extends Symbol> List<T> getSymbolsOfType(@NotNull Class<T> type) {
            return new ArrayList<>();
        }

        @Override
        @NotNull
        public List<Symbol> getSymbolsOfType(@NotNull Class<?>... type) {
            return new ArrayList<>();
        }
    }
}
