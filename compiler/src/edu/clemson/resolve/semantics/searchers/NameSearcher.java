package edu.clemson.resolve.semantics.searchers;

import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;
import java.util.Map;

public class NameSearcher implements MultimatchTableSearcher<Symbol> {

    @NotNull private final String searchString;
    private final boolean stopAfterFirst;

    public NameSearcher(@NotNull String searchString, boolean stopAfterFirst) {
        this.searchString = searchString;
        this.stopAfterFirst = stopAfterFirst;
    }

    public NameSearcher(@NotNull String searchString) {
        this(searchString, true);
    }

    @Override public boolean addMatches(@NotNull Map<String, Symbol> entries,
                                        @NotNull List<Symbol> matches,
                                        @NotNull SearchContext l) {

        boolean result = entries.containsKey(searchString);
        if (result) {
            Symbol e = entries.get(searchString);

            //TODO
            //Parameters of imported modules or facility instantiations ar not
            //exported and therefore should not be considered for results
            if (l.equals(SearchContext.SOURCE_MODULE)
                    || !(e instanceof ProgParameterSymbol)) {
                matches.add(entries.get(searchString));
            }
        }
        return stopAfterFirst && result;
    }
}
