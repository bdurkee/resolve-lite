package resolvelite.typeandpopulate;

import resolvelite.typeandpopulate.entry.SymbolTableEntry;

import java.util.*;

public abstract class AbstractScope implements Scope {

    @Override
    public final <E extends SymbolTableEntry> List<E> getMatches(
            TableSearcher<E> searcher, TableSearcher.SearchContext l)
            throws DuplicateSymbolException {
        List<E> result = new LinkedList<E>();
        Set<Scope> searchedScopes = new HashSet<Scope>();

        addMatches(searcher, result, searchedScopes, null, l);
        return result;
    }

}
