package org.resolvelite.semantics;

import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.query.UnqualifiedNameQuery;
import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.searchers.TableSearcher.SearchContext;
import org.resolvelite.semantics.symbol.FacilitySymbol;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;


import java.util.List;

public class QualifiedPath implements ScopeSearchPath {

    private final FacilityStrategy facilityStrategy;
    private final Token qualifier;

    public QualifiedPath(Token qualifier, FacilityStrategy facilityStrategy) {

        if (facilityStrategy == FacilityStrategy.FACILITY_IGNORE) {
            throw new IllegalArgumentException("can't use facility ignore "
                    + "strategy in performing a qualified search");
        }
        this.facilityStrategy = facilityStrategy;
        this.qualifier = qualifier;
    }

    @Override
    public <E extends Symbol> List<E> searchFromContext(
            TableSearcher<E> searcher, Scope source, SymbolTable repo)
            throws DuplicateSymbolException {
        List<E> result;
        try {
            FacilitySymbol facilitySym =
                    (FacilitySymbol)
                            source.queryForOne(
                                    new UnqualifiedNameQuery(qualifier.getText()));
        } catch (NoSuchSymbolException e) {
            //then perhaps it identifies a module..
            try {
                ModuleScopeBuilder moduleScope =
                        repo.getModuleScope(qualifier.getText());

                result = moduleScope.getMatches(searcher, TableSearcher.SearchContext.IMPORT);
            }
            catch (NoSuchSymbolException nsse2) {
                throw new NoSuchSymbolException();
            }
        }
    }
}
