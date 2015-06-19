package edu.clemson.resolve.compiler;

import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.*;
import java.util.stream.Collectors;

public class ImportCollection {

    public static enum ImportType { NAMED, IMPLICIT, EXTERNAL }

    private final Map<ImportType, LinkedHashSet<String>> imports =
            new HashMap<>();

    public ImportCollection() {
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
     * @param type Any types we would like to filter/exclude.
     * @return A set of imports filtered by <code>type</code>.
     */
    public Set<String> getImportsExcluding(ImportType... type) {
        Set<String> result = new HashSet<>();
        List<ImportType> typesToExclude = Arrays.asList(type);

        for (ImportType s : imports.keySet()) {
            if ( !typesToExclude.contains(s) ) {
                result.addAll(imports.get(s));
            }
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
        List<String> convertedTerms = new ArrayList<>();
        for (TerminalNode t : terminals) {
            convertedTerms.add(t.getSymbol().getText());
        }
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
        //but is later added as an explicit import too, then it the import
        //should appear twice in two different categories (especially if
        // it's already listed as an implicit import). So in other words,
        //cull duplicate references in multiple categories...
        tokSet.addAll(newToks);
    }

    /**
     * Returns <code>true</code> <strong>iff</strong> the set of
     * <code>type</code> imports contains <code>t</code>; <code>false</code>
     * otherwise.
     *
     * @param type A {@link ImportType}.
     * @param t A name token.
     *
     * @return <code>true</code> if <code>t</code> is in the set of
     *         <code>type</code>, <code>false</code> otherwise.
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
