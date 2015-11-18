package org.rsrg.semantics;

import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.MathSymbolTableBuilder.FacilityStrategy;
import org.rsrg.semantics.MathSymbolTableBuilder.ImportStrategy;
import org.rsrg.semantics.searchers.TableSearcher;
import org.rsrg.semantics.symbol.Symbol;

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
            TableSearcher<E> searcher, Scope source, MathSymbolTableBuilder repo)
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
