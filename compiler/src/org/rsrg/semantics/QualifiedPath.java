package org.rsrg.semantics;

import edu.clemson.resolve.compiler.ErrorKind;
import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.SymbolTable.FacilityStrategy;
import org.rsrg.semantics.query.UnqualifiedNameQuery;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.searchers.TableSearcher.SearchContext;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public class QualifiedPath implements ScopeSearchPath {

    private final boolean instantiateGenerics;
    private final Token qualifier;

    public QualifiedPath(Token qualifier, FacilityStrategy facilityStrategy) {
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
                            .queryForOne(new UnqualifiedNameQuery(
                                    qualifier.getText()));

            Scope facilityScope = facility.getFacility().getSpecification() //
                    .getScope(instantiateGenerics);
            result = facilityScope.getMatches(searcher, SearchContext.FACILITY);
        }
        catch (NoSuchSymbolException e) {
            //then perhaps it identifies a module..

            ModuleScopeBuilder moduleScope = repo.moduleScopes.get(
                    qualifier.getText());
            if (moduleScope == null) {
                System.out.println("NO SUCH MODULE: " + qualifier);
                repo.getCompiler().errMgr.semanticError(
                        ErrorKind.NO_SUCH_MODULE, qualifier, qualifier.getText());
                return result;
            }
            result =
                    moduleScope.getMatches(searcher,
                            TableSearcher.SearchContext.IMPORT);

        }
        return result;
    }
}
