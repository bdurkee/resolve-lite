package edu.clemson.resolve.compiler;

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
public class ImportCollection {

    public static enum ImportType { NAMED, IMPLICIT, EXTERNAL }

    private final Map<ImportType, LinkedHashSet<String>> imports =
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
    public Set<String> getImportsExcluding(ImportType... type) {
        Set<String> result = new HashSet<>();
        List<ImportType> typesToExclude = Arrays.asList(type);
        for (ImportType s : imports.keySet()) {
            if ( !typesToExclude.contains(s) ) result.addAll(imports.get(s));
        }
        return result;
    }

    public List<String> getImportsOfType(ImportType type) {
        return new ArrayList<>(imports.get(type));
    }

    public void imports(ImportType type, String... t) {
        addTokenSet(type, Arrays.asList(t));
    }

    public void imports(ImportType type, TerminalNode... t) {
        imports(type, Arrays.asList(t));
    }

    public void imports(ImportType type, List<TerminalNode> terminals) {
        List<String> convertedTerms = terminals.stream()
                .map(t -> t.getSymbol().getText()).collect(Collectors.toList());
        addTokenSet(type, convertedTerms);
    }

    public void addImports(ImportType type, String... t) {
        addTokenSet(type, Arrays.asList(t));
    }

    public void addTokenSet(ImportType type, Collection<String> newToks) {
        LinkedHashSet<String> tokSet = imports.get(type);
        if ( tokSet == null ) {
            tokSet = new LinkedHashSet<String>();
        }
        //Todo: Do a little normalization here on additions. For instance,
        //if something already exists in the map as an implicit import,
        //but is later added as an explicit import too, then the import
        //will appear twice in two different categories (especially if
        // it's already listed as an implicit import). So in other words,
        //we need cull duplicate references in multiple categories...
        tokSet.addAll(newToks);
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

    public Set<String> getAllImports() {
        Set<String> aggregateImports = new HashSet<>();

        for (Set<String> typeSet : imports.values()) {
            aggregateImports.addAll(typeSet);
        }
        return aggregateImports;
    }
}
