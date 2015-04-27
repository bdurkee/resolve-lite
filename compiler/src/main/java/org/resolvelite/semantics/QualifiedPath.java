package org.resolvelite.semantics;

import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.query.UnqualifiedNameQuery;
import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.searchers.TableSearcher.SearchContext;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;

import java.util.ArrayList;
import java.util.List;

public class QualifiedPath implements ScopeSearchPath {

    private final boolean instantiateGenerics;
    private final String qualifier;

    public QualifiedPath(String qualifier, FacilityStrategy facilityStrategy) {
        this.instantiateGenerics =
                facilityStrategy == FacilityStrategy.FACILITY_INSTANTIATE;
        this.qualifier = qualifier;
    }

    @Override public <E extends Symbol> List<E> searchFromContext(
            TableSearcher<E> searcher, Scope source, SymbolTable repo)
            throws DuplicateSymbolException {
        List<E> result = new ArrayList<>();
        try {
            FacilitySymbol facility =
                    (FacilitySymbol) source
                            .queryForOne(new UnqualifiedNameQuery(qualifier));

            Scope facilityScope = facility.getFacility().getSpecification() //
                    .getScope(instantiateGenerics);
            result = facilityScope.getMatches(searcher, SearchContext.FACILITY);
        }
        catch (NoSuchSymbolException e) {
            //then perhaps it identifies a module..
            try {
                ModuleScopeBuilder moduleScope = repo.getModuleScope(qualifier);
                result =
                        moduleScope.getMatches(searcher,
                                TableSearcher.SearchContext.IMPORT);
            }
            catch (NoSuchSymbolException nsse2) {}
        }
        return result;
    }
}
