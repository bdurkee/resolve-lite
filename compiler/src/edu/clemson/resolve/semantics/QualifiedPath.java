package edu.clemson.resolve.semantics;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy;
import edu.clemson.resolve.semantics.query.UnqualifiedNameQuery;
import edu.clemson.resolve.semantics.searchers.TableSearcher;
import edu.clemson.resolve.semantics.searchers.TableSearcher.SearchContext;
import edu.clemson.resolve.semantics.symbol.FacilitySymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public class QualifiedPath implements ScopeSearchPath {

    @NotNull
    private final Token qualifier;
    private final boolean instantiateGenerics;

    public QualifiedPath(@NotNull Token qualifier, @NotNull FacilityStrategy facilityStrategy) {
        this.instantiateGenerics = facilityStrategy == FacilityStrategy.FACILITY_INSTANTIATE;
        this.qualifier = qualifier;
    }

    @Override
    @NotNull
    public <E extends Symbol> List<E> searchFromContext(@NotNull TableSearcher<E> searcher,
                                                        @NotNull Scope source,
                                                        @NotNull MathSymbolTable repo)
            throws DuplicateSymbolException, NoSuchModuleException, UnexpectedSymbolException {
        List<E> result = new ArrayList<>();
        try {
            FacilitySymbol facility =
                    (FacilitySymbol) source.queryForOne(new UnqualifiedNameQuery(qualifier.getText()));

            Scope facilityScope = facility.getFacility()
                    .getSpecification()
                    .getScope(instantiateGenerics);
            result = facilityScope.getMatches(searcher, SearchContext.FACILITY);
            //Dtw returnEnsuresArgSubstitutions:
            for (ModuleParameterization enh : facility.getEnhancements()) {
                Scope enhScope = enh.getScope(instantiateGenerics);
                result.addAll(enhScope.getMatches(searcher, SearchContext.FACILITY));
            }
        } catch (NoSuchSymbolException | ClassCastException e) {
            //then perhaps it identifies a module..
            ModuleScopeBuilder sourceModuleScope = repo.getModuleScope(source.getModuleIdentifier());
            ModuleIdentifier qualifierModuleIdentifier = null;
            try {
                qualifierModuleIdentifier = sourceModuleScope.getImportWithName(qualifier);
            } catch (NoSuchModuleException e2) {
                //let's try to get its alias (note, if there is no alias with this name,
                //this getter will throw the appropriate no such module exception
                qualifierModuleIdentifier = sourceModuleScope.getAlias(qualifier);
            }
            //Note that if qualifierModuleIdentifier can't null here, a no such module exception would've been
            //thrown in the prior try catch block.
            ModuleScopeBuilder referencedModuleScope = repo.getModuleScope(qualifierModuleIdentifier);
            result = referencedModuleScope.getMatches(searcher, TableSearcher.SearchContext.IMPORT);
        }
        return result;
    }
}
