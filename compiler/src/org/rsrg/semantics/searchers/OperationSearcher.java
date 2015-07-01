package org.rsrg.semantics.searchers;

import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.DuplicateSymbolException;
import org.rsrg.semantics.UnexpectedSymbolException;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.symbol.OperationSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
            try {
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
            catch (UnexpectedSymbolException use) {}
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
