package edu.clemson.resolve.semantics.searchers;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.DuplicateSymbolException;
import edu.clemson.resolve.semantics.UnexpectedSymbolException;
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.semantics.symbol.OperationSymbol;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OperationSearcher implements TableSearcher<OperationSymbol> {

    @NotNull private final String queryName;
    @NotNull private final List<ProgType> actualArgTypes;

    public OperationSearcher(@NotNull Token name,
                             @NotNull List<ProgType> argumentTypes) {
        this(name.getText(), argumentTypes);
    }

    public OperationSearcher(@NotNull String name,
                             @NotNull List<ProgType> argumentTypes) {
        this.queryName = name;
        this.actualArgTypes = new ArrayList<>(argumentTypes);
    }

    @Override public boolean addMatches(@NotNull Map<String, Symbol> entries,
                                        @NotNull List<OperationSymbol> matches,
                                        @NotNull SearchContext l)
            throws DuplicateSymbolException {

        if (entries.containsKey(queryName)) {
            try {
                OperationSymbol operation =
                        entries.get(queryName).toOperationSymbol();

                if ( argumentsMatch(operation.getParameters()) ) {
                    //We have a match at this point
                    if (!matches.isEmpty()) {
                        throw new DuplicateSymbolException();
                    }
                    matches.add(operation);
                }
            }
            catch (UnexpectedSymbolException use) {}
        }
        return false;
    }

    private boolean argumentsMatch(
            @NotNull List<ProgParameterSymbol> formalParameters) {

        boolean result = (formalParameters.size() == actualArgTypes.size());

        if (result) {
            Iterator<ProgParameterSymbol> formalParametersIter =
                    formalParameters.iterator();
            Iterator<ProgType> actualArgumentTypeIter = actualArgTypes.iterator();

            ProgType actualArgumentType, formalParameterType;
            while (result && formalParametersIter.hasNext()) {
                actualArgumentType = actualArgumentTypeIter.next();
                formalParameterType =
                        formalParametersIter.next().getDeclaredType();

                result = actualArgumentType.acceptableFor(formalParameterType);
            }
        }
        return result;
    }
}
