package edu.clemson.resolve.semantics;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.MathSymbolTable.FacilityStrategy;
import edu.clemson.resolve.semantics.MathSymbolTable.ImportStrategy;
import edu.clemson.resolve.semantics.searchers.TableSearcher;
import edu.clemson.resolve.semantics.symbol.Symbol;

import java.util.List;

/**
 * Like the name suggests, represents an implementation {@link ScopeSearchPath}
 * that might (or might not be) qualified.
 * <p>
 * The implementation of
 * {@link #searchFromContext(TableSearcher, Scope, MathSymbolTable)} here
 * abstracts which of the two are selected based on the parameters passed to
 * {@code this}.</p>
 */
public class PossiblyQualifiedPath implements ScopeSearchPath {

    @NotNull
    private final ScopeSearchPath actualSearchPath;

    public PossiblyQualifiedPath(@Nullable Token qualifier,
                                 @NotNull ImportStrategy importStrategy,
                                 @NotNull FacilityStrategy facilityStrategy,
                                 boolean localPriority) {
        this.actualSearchPath =
                getAppropriatePath(qualifier, importStrategy, facilityStrategy,
                        localPriority);
    }

    public PossiblyQualifiedPath(@Nullable Token qualifier) {
        this(qualifier, ImportStrategy.IMPORT_NONE,
                FacilityStrategy.FACILITY_IGNORE, false);
    }

    @NotNull
    @Override
    public <E extends Symbol> List<E> searchFromContext(
            @NotNull TableSearcher<E> searcher, @NotNull Scope source,
            @NotNull MathSymbolTable repo)
            throws DuplicateSymbolException, NoSuchModuleException,
            UnexpectedSymbolException {
        return actualSearchPath.searchFromContext(searcher, source, repo);
    }

    @NotNull
    private static ScopeSearchPath getAppropriatePath(
            @Nullable Token qualifier,
            @NotNull ImportStrategy importStrategy,
            @NotNull FacilityStrategy facilityStrategy,
            boolean localPriority) {
        ScopeSearchPath result;
        if (qualifier == null) {
            result =
                    new UnqualifiedPath(importStrategy, facilityStrategy,
                            localPriority);
        } else {
            result = new QualifiedPath(qualifier, facilityStrategy);
        }
        return result;
    }
}
