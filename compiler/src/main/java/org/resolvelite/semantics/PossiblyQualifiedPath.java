package org.resolvelite.semantics;

import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.searchers.TableSearcher;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;

import java.util.List;

public class PossiblyQualifiedPath implements ScopeSearchPath {

    private final ScopeSearchPath actualSearchPath;

    public PossiblyQualifiedPath(Token qualifier,
            ImportStrategy importStrategy, FacilityStrategy facilityStrategy,
            boolean localPriority) {
        this.actualSearchPath =
                getAppropriatePath(qualifier, importStrategy, facilityStrategy,
                        localPriority);
    }

    public PossiblyQualifiedPath(Token qualifier) {
        this(qualifier, ImportStrategy.IMPORT_NONE,
                FacilityStrategy.FACILITY_IGNORE, false);
    }

    @Override public <E extends Symbol> List<E> searchFromContext(
            TableSearcher<E> searcher, Scope source, SymbolTable repo)
            throws DuplicateSymbolException {
        return actualSearchPath.searchFromContext(searcher, source, repo);
    }

    private static ScopeSearchPath getAppropriatePath(Token qualifier,
            ImportStrategy importStrategy, FacilityStrategy facilityStrategy,
            boolean localPriority) {
        ScopeSearchPath result;

        if ( qualifier == null ) {
            result =
                    new UnqualifiedPath(importStrategy, facilityStrategy,
                            localPriority);
        }
        else {
            result = new QualifiedPath(qualifier, facilityStrategy);
        }
        return result;
    }
}
