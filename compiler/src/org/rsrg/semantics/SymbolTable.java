package org.rsrg.semantics;

import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.compiler.RESOLVECompiler;
import edu.clemson.resolve.misc.HardCoded;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.proving.absyn.PExp;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class SymbolTable {

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
    public final Map<String, ModuleScopeBuilder> moduleScopes = new HashMap<>();

    public final ParseTreeProperty<PExp> mathPExps = //
            new ParseTreeProperty<>();
    public final ParseTreeProperty<ScopeBuilder> scopes =
            new ParseTreeProperty<>();

    private ModuleScopeBuilder curModuleScope = null;
    private final TypeGraph typeGraph;
    private final RESOLVECompiler compiler;

    public SymbolTable(RESOLVECompiler rc) {
        this.typeGraph = new TypeGraph();
        this.compiler = rc;

        //The only things in global scope are built-in things
        ScopeBuilder globalScope =
                new ScopeBuilder(this, typeGraph, null, DUMMY_RESOLVER,
                        "GLOBAL");

        HardCoded.addBuiltInSymbols(typeGraph, rc, globalScope);
        lexicalScopeStack.push(globalScope);
    }

    public TypeGraph getTypeGraph() {
        return typeGraph;
    }

    public RESOLVECompiler getCompiler() {
        return compiler;
    }

    public ModuleScopeBuilder startModuleScope(
            ParserRuleContext ctx, String name) {

        ParseTree moduleTree = null;
        if ( curModuleScope != null ) {
            throw new IllegalStateException("module scope already open");
        }
        if ( ctx instanceof Resolve.ModuleContext ) {
            moduleTree = ctx.getChild(0);
        }
        else if ( !(ctx.getParent() instanceof Resolve.ModuleContext) ) {
            throw new IllegalArgumentException("the rule context "
                    + "isn't for a module");
        }

        ScopeBuilder parent = lexicalScopeStack.peek();
        ModuleScopeBuilder s =
                new ModuleScopeBuilder(typeGraph, name, ctx, parent, this);
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

    public ModuleScopeBuilder getModuleScope(String name)
            throws NoSuchSymbolException {
        ModuleScopeBuilder module = moduleScopes.get(name);
        return module;
    }
}
