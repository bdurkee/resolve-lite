package edu.clemson.resolve.compiler;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Maintains disjoint sets of module uses references. References currently come
 * in three flavors:
 * <ul>
 * <li>{@code named}: Explicitly named imports (e.g. uses x,y).</li>
 * <li>{@code implicit}: Those that come via facility declarations.</li>
 * <li>{@code external}: Those preceded by the {@code external} keyword.</li>
 * </ul>
 * <p>
 * If the same reference appears in two separate categories
 * (where one is of type {@code named}), we always default to named--since it's
 * the 'stronger' category (and should have the same affect anyways). However
 * if the overlap involves an {@code external} reference, then that's an error;
 * our language can't load external files atm and the only place they should
 * appear is in the context of a facility declaration.</p>
 */

//Todo: Since adding ImportReference object, this class should perhaps be rethought.
    //maybe add a field for ImportType to ImportReference? That seems like it should be there.
public class ImportCollection {

    public static enum ImportType { NAMED, IMPLICIT, EXTERNAL }

    public static class ImportReference {
        public Token location;
        public String name;
        public ImportReference(Token loc, String name) {
            this.location = loc;
            this.name = name;
        }

        @Override public int hashCode() {
            return name.hashCode();
        }
        @Override public boolean equals(Object o) {
            boolean result = (o instanceof ImportReference);
            if (result) {
                result = ((ImportReference)o).name.equals(this.name);
            }
            return result;
        }
    }
    private final Map<ImportType, LinkedHashSet<ImportReference>> imports =
            new HashMap<>();

    public ImportCollection() {
        //Initialize all categories to the empty set.
        for (int i = 0; i < ImportType.values().length; i++) {
            ImportType curType = ImportType.values()[i];
            if ( imports.get(curType) == null ) {
                imports.put(curType, new LinkedHashSet<>());
            }
        }
    }

    /**
     * Retrieves a set of all imports except those of {@code type}.
     *
     * @param type {@code ImportType}s to filter by.
     * @return a set of filtered import .
     */
    public Set<ImportReference> getImportsExcluding(ImportType... type) {
        Set<ImportReference> result = new HashSet<>();
        List<ImportType> typesToExclude = Arrays.asList(type);
        for (ImportType s : imports.keySet()) {
            if ( !typesToExclude.contains(s) ) result.addAll(imports.get(s));
        }
        return result;
    }

    public List<ImportReference> getImportsOfType(ImportType type) {
        return new ArrayList<>(imports.get(type));
    }

    public void imports(ImportType type, Token... t) {
        List<Token> starting = Arrays.asList(t);
        List<ImportReference> result = new ArrayList<>();
        for (Token e : starting) {
            result.add(new ImportReference(e, e.getText()));
        }
        addTokenSet(type, result);
    }

    public void imports(ImportType type, ImportReference... t) {
        addTokenSet(type, Arrays.asList(t));
    }

    public void imports(ImportType type, TerminalNode... t) {
        imports(type, Arrays.asList(t));
    }

    public void imports(ImportType type, List<TerminalNode> terminals) {
        List<ImportReference> convertedTerms = terminals.stream()
                .map(t -> new ImportReference(t.getSymbol(), t.getText()))
                .collect(Collectors.toList());
        addTokenSet(type, convertedTerms);
    }

    public void addImports(ImportType type, ImportReference... t) {
        addTokenSet(type, Arrays.asList(t));
    }

    public void addTokenSet(ImportType type, Collection<ImportReference> newToks) {
        LinkedHashSet<ImportReference> tokSet = imports.get(type);
        if ( tokSet == null ) {
            tokSet = new LinkedHashSet<ImportReference>();
        }
        //Todo: Do a little normalization here on additions. For instance,
        //if something already exists in the map as an implicit import,
        //but is later added as an explicit import too, then the import
        //will appear twice in two different categories (especially if
        // it's already listed as an implicit import). So in other words,
        //we need cull duplicate references in multiple categories...
        tokSet.addAll(newToks);
    }

    public void addStringSet(ImportType type, Collection<String> l) {
        LinkedHashSet<ImportReference> tokSet = imports.get(type);
        if ( tokSet == null ) {
            tokSet = new LinkedHashSet<ImportReference>();
        }
        //Todo: Do a little normalization here on additions. For instance,
        //if something already exists in the map as an implicit import,
        //but is later added as an explicit import too, then the import
        //will appear twice in two different categories (especially if
        // it's already listed as an implicit import). So in other words,
        //we need cull duplicate references in multiple categories...
        for (String s : l) {
            tokSet.add(new ImportReference(null, s));
        }
    }

    /**
     * Returns {@code true} iff the set of imports of {@code type} contains
     * {@code t}; {@code false} otherwise.
     *
     * @param type an import type
     * @param t a name token
     * @return is {@code t} in category {@code type}?
     */
    public boolean inCategory(ImportType type, String t) {
        return imports.get(type).contains(t);
    }

    public Set<ImportReference> getAllImports() {
        Set<ImportReference> aggregateImports = new HashSet<>();

        for (Set<ImportReference> typeSet : imports.values()) {
            aggregateImports.addAll(typeSet);
        }
        return aggregateImports;
    }
}
