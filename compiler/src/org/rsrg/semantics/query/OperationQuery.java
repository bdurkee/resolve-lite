package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.PossiblyQualifiedPath;
import org.rsrg.semantics.programtype.ProgType;
import org.rsrg.semantics.searchers.OperationSearcher;
import org.rsrg.semantics.symbol.OperationSymbol;

import java.util.List;

import static org.rsrg.semantics.MathSymbolTable.*;

/**
 * An {@code OperationQuery} searches for a (possibly-qualified) operation.
 * If a qualifier is provided, the named facility or module is searched.
 * Otherwise, the operation is searched for in any directly imported modules and
 * in instantiated versions of any available facilities.
 */
public class OperationQuery extends BaseSymbolQuery<OperationSymbol> {

    public OperationQuery(@Nullable Token qualifier, @NotNull Token name,
                          @NotNull List<ProgType> argumentTypes,
                          @NotNull FacilityStrategy facilityStrategy,
                          @NotNull ImportStrategy importStrategy) {
        super(new PossiblyQualifiedPath(qualifier, importStrategy,
                facilityStrategy, false), new OperationSearcher(name,
                argumentTypes));
    }

    public OperationQuery(@Nullable Token qualifier, @NotNull String name,
                          @NotNull List<ProgType> argumentTypes) {
        super(new PossiblyQualifiedPath(qualifier, ImportStrategy.IMPORT_NAMED,
                        FacilityStrategy.FACILITY_INSTANTIATE, false),
                new OperationSearcher(name, argumentTypes));
    }

    public OperationQuery(@Nullable Token qualifier, @NotNull Token name,
                          @NotNull List<ProgType> argumentTypes) {
        this(qualifier, name.getText(), argumentTypes);
       /* super(new PossiblyQualifiedPath(qualifier, ImportStrategy.IMPORT_NAMED,
                FacilityStrategy.FACILITY_IGNORE, false),
                new OperationSearcher(name, argumentTypes));*/
    }
}