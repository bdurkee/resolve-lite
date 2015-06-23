package org.rsrg.semantics;

import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.SymbolTable.FacilityStrategy;
import org.rsrg.semantics.SymbolTable.ImportStrategy;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.symbol.Symbol;

import java.util.List;

public class PossiblyQualifiedPath implements ScopeSearchPath {

    private final ScopeSearchPath actualSearchPath;

    public PossiblyQualifiedPath(Token qualifier,
            ImportStrategy importStrategy, FacilityStrategy facilityStrategy,
            boolean localPriority) {
        this(qualifier != null ? qualifier.getText() : null, importStrategy,
                facilityStrategy, localPriority);
    }

    public PossiblyQualifiedPath(String qualifier,
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

    private static ScopeSearchPath getAppropriatePath(String qualifier,
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
