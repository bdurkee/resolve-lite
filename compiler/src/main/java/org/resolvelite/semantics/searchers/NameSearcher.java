package org.resolvelite.semantics.searchers;

import org.resolvelite.semantics.symbol.GenericSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;

public class NameSearcher implements MultimatchTableSearcher<Symbol> {

    private final String searchString;
    private final boolean stopAfterFirst;

    public NameSearcher(String searchString, boolean stopAfterFirst) {
        this.searchString = searchString;
        this.stopAfterFirst = stopAfterFirst;
    }

    public NameSearcher(String searchString) {
        this(searchString, true);
    }

    @Override public boolean addMatches(Map<String, Symbol> entries,
            List<Symbol> matches, SearchContext l) {

        boolean result = entries.containsKey(searchString);
        if ( result ) {
            Symbol e = entries.get(searchString);

            //Parameters of imported modules or facility instantiations ar not
            //exported and therefore should not be considered for results
            if ( l.equals(SearchContext.SOURCE_MODULE)
                    || !(e instanceof ProgParameterSymbol) ) {
                matches.add(entries.get(searchString));
            }
        }
        return stopAfterFirst && result;
    }
}
