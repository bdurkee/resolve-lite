package org.rsrg.semantics;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MathSymbolTable.FacilityStrategy;
import org.rsrg.semantics.query.UnqualifiedNameQuery;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.searchers.TableSearcher.SearchContext;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public class QualifiedPath implements ScopeSearchPath {

    @NotNull private final Token qualifier;
    private final boolean instantiateGenerics;

    public QualifiedPath(@NotNull Token qualifier,
                         @NotNull FacilityStrategy facilityStrategy) {
        this.instantiateGenerics =
                facilityStrategy == FacilityStrategy.FACILITY_INSTANTIATE;
        this.qualifier = qualifier;
    }

    @Override @NotNull public <E extends Symbol> List<E> searchFromContext(
            @NotNull TableSearcher<E> searcher, @NotNull Scope source,
            @NotNull MathSymbolTable repo)
            throws DuplicateSymbolException, NoSuchModuleException {
        List<E> result = new ArrayList<>();
        try {
            FacilitySymbol facility =
                    (FacilitySymbol) source
                            .queryForOne(new UnqualifiedNameQuery(
                                    qualifier.getText()));

            Scope facilityScope = facility.getFacility().getSpecification() //
                    .getScope(instantiateGenerics);
            result = facilityScope.getMatches(searcher, SearchContext.FACILITY);
            //Dtw test:
            for (ModuleParameterization enh : facility.getEnhancements()) {
                Scope enhScope = enh.getScope(instantiateGenerics);
                result.addAll(enhScope.getMatches(searcher,
                        SearchContext.FACILITY));
            }
        }
        catch (NoSuchSymbolException e) {
            //then perhaps it identifies a module..
            ModuleScopeBuilder moduleScope =
                    repo.getModuleScope(new ModuleIdentifier(qualifier));
            result =
                    moduleScope.getMatches(searcher,
                            TableSearcher.SearchContext.IMPORT);

        }
        return result;
    }
}
