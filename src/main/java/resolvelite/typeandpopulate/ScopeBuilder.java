package resolvelite.typeandpopulate;

import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.ResolveCompiler;
import resolvelite.compiler.ErrorKind;
import resolvelite.typeandpopulate.entry.MathSymbolEntry;
import resolvelite.typeandpopulate.entry.SymbolTableEntry;
import resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;

public class ScopeBuilder implements Scope {

    private final Map<String, SymbolTableEntry> bindings;

    protected final TypeGraph typeGraph;
    protected final ParseTree definingElement;
    protected Scope parent;
    private final MathSymbolTableBuilder source;

    private final ResolveCompiler compiler;

    public ScopeBuilder(MathSymbolTableBuilder b, TypeGraph g,
                        ParseTree definingElement, Scope parent,
                        Map<String, SymbolTableEntry> bindings) {
        this.source = b;
        this.typeGraph = g;
        this.definingElement = definingElement;
        this.parent = parent;
        this.bindings = bindings;
        this.compiler = source.getCompiler();
        initMathTypeSystem();
    }

    protected void initMathTypeSystem() {
        try {

            addBinding("", null, null);

        }
        catch (DuplicateSymbolException dse) {
            //shouldn't happen, we're first ones to add anything.
        }
    }

    @Override
    public String getScopeName() {
        return "global";
    }

    @Override
    public Scope getEnclosingScope() {
        return null;
    }

    public MathSymbolEntry addBinding(String name,
                                      SymbolTableEntry.Quantification q,
                                      ParseTree definingElement,
                                      MTType type, MTType typeValue,
                                      Map<String, MTType> schematicTypes)
            throws DuplicateSymbolException {
        sanityCheckBindArguments(name, definingElement, type);

        MathSymbolEntry entry =
                new MathSymbolEntry(source.getCompiler(),
                        typeGraph, name, q, definingElement,
                        type, typeValue, schematicTypes);
        bindings.put(name, entry);
        return entry;
    }

    public MathSymbolEntry addBinding(String name,
                                      SymbolTableEntry.Quantification q,
                                      ParseTree definingElement,
                                      MTType type) throws DuplicateSymbolException {
        return addBinding(name, q, definingElement, type, null, null);
    }

    public MathSymbolEntry addBinding(String name, ParseTree definingElement,
                                      MTType type, MTType typeValue)
            throws DuplicateSymbolException {

        return addBinding(name, SymbolTableEntry.Quantification.NONE,
                definingElement, type, typeValue, null);
    }

    public MathSymbolEntry addBinding(String name, ParseTree definingElement,
                                      MTType type) throws DuplicateSymbolException {

        return addBinding(name, SymbolTableEntry.Quantification.NONE,
                definingElement, type);
    }

    @Override
    public SymbolTableEntry resolve(String name) {
        return bindings.get(name);
    }

    @Override
    public String toString() {
        return getScopeName() + ":" + bindings;
    }

    private void sanityCheckBindArguments(String name,
                                          ParseTree definingElement,
                                          Object type)
            throws DuplicateSymbolException {
        SymbolTableEntry curLocalEntry = bindings.get(name);
        if (curLocalEntry != null) {
            throw new DuplicateSymbolException(curLocalEntry);
        }
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("symbol table entry name must "
                    + "be non-null and contain at least one character");
        }
        if (type == null) {
            throw new IllegalArgumentException("symbol table entry type must "
                    + "be non-null.");
        }
    }
}
