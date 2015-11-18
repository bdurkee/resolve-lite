package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.MathSymbolTableBuilder;
import org.rsrg.semantics.PossiblyQualifiedPath;
import org.rsrg.semantics.programtype.PTType;
import org.rsrg.semantics.searchers.OperationSearcher;
import org.rsrg.semantics.symbol.OperationSymbol;

import java.util.List;

/**
 * An {@code OperationQuery} searches for a (possibly-qualified) operation.
 * If a qualifier is provided, the named facility or module is searched.
 * Otherwise, the operation is searched for in any directly imported modules and
 * in instantiated versions of any available facilities.
 */
public class OperationQuery extends BaseSymbolQuery<OperationSymbol> {

    public OperationQuery(Token qualifier, Token name,
                          List<PTType> argumentTypes, MathSymbolTableBuilder.FacilityStrategy facilityStrategy,
                          MathSymbolTableBuilder.ImportStrategy importStrategy) {
        super(new PossiblyQualifiedPath(qualifier, importStrategy,
                facilityStrategy, false), new OperationSearcher(name,
                argumentTypes));
    }

    public OperationQuery(Token qualifier, Token name,
                          List<PTType> argumentTypes) {
        super(new PossiblyQualifiedPath(qualifier, MathSymbolTableBuilder.ImportStrategy.IMPORT_NAMED,
                MathSymbolTableBuilder.FacilityStrategy.FACILITY_IGNORE, false),
                new OperationSearcher(name, argumentTypes));
    }

    public OperationQuery(Token qualifier, String name,
                          List<PTType> argumentTypes) {
        super(new PossiblyQualifiedPath(qualifier, MathSymbolTableBuilder.ImportStrategy.IMPORT_NAMED,
                MathSymbolTableBuilder.FacilityStrategy.FACILITY_INSTANTIATE, false),
                new OperationSearcher(name, argumentTypes));
    }

}