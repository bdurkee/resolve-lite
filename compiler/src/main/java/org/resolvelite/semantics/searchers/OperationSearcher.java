package org.resolvelite.semantics.searchers;

import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.symbol.OperationSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.*;

public class OperationSearcher implements TableSearcher<OperationSymbol> {

    private final String queryName;
    private final List<PTType> actualArgTypes;

    public OperationSearcher(Token name, List<PTType> argumentTypes) {
        this(name.getText(), argumentTypes);
    }

    public OperationSearcher(String name, List<PTType> argumentTypes) {
        this.queryName = name;
        this.actualArgTypes = new ArrayList<>(argumentTypes);
    }

    @Override public boolean addMatches(Map<String, Symbol> entries,
            List<OperationSymbol> matches, SearchContext l)
            throws DuplicateSymbolException {

        if ( entries.containsKey(queryName) ) {
            OperationSymbol operation =
                    entries.get(queryName).toOperationSymbol();

            if ( argumentsMatch(operation.getParameters()) ) {
                //We have a match at this point
                if ( !matches.isEmpty() ) {
                    throw new DuplicateSymbolException();
                }

                matches.add(operation);
            }
        }
        return false;
    }

    private boolean argumentsMatch(List<ProgParameterSymbol> formalParameters) {

        boolean result = (formalParameters.size() == actualArgTypes.size());

        if ( result ) {
            Iterator<ProgParameterSymbol> formalParametersIter =
                    formalParameters.iterator();
            Iterator<PTType> actualArgumentTypeIter = actualArgTypes.iterator();

            PTType actualArgumentType, formalParameterType;
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
