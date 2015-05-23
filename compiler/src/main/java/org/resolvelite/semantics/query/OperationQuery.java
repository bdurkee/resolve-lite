package org.resolvelite.semantics.query;

import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.PossiblyQualifiedPath;
import org.resolvelite.semantics.SymbolTable;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.searchers.OperationSearcher;
import org.resolvelite.semantics.symbol.OperationSymbol;

import java.util.List;

/**
 * An {@code OperationQuery} searches for a (possibly-qualified) operation.
 * If a qualifier is provided, the named facility or module is searched.
 * Otherwise, the operation is searched for in any directly imported modules and
 * in instantiated versions of any available facilities.
 */
public class OperationQuery extends BaseSymbolQuery<OperationSymbol> {

    public OperationQuery(Token qualifier, Token name,
            List<PTType> argumentTypes, FacilityStrategy facilityStrategy,
            ImportStrategy importStrategy) {
        super(new PossiblyQualifiedPath(qualifier, importStrategy,
                facilityStrategy, false), new OperationSearcher(name,
                argumentTypes));
    }

    public OperationQuery(Token qualifier, Token name,
            List<PTType> argumentTypes) {
        super(new PossiblyQualifiedPath(qualifier, ImportStrategy.IMPORT_NAMED,
                FacilityStrategy.FACILITY_IGNORE, false),
                new OperationSearcher(name, argumentTypes));
    }

    public OperationQuery(Token qualifier, String name,
            List<PTType> argumentTypes) {
        super(new PossiblyQualifiedPath(qualifier, ImportStrategy.IMPORT_NAMED,
                FacilityStrategy.FACILITY_IGNORE, false),
                new OperationSearcher(name, argumentTypes));
    }

    public OperationQuery(String qualifier, String name,
            List<PTType> argumentTypes) {
        super(new PossiblyQualifiedPath(qualifier, ImportStrategy.IMPORT_NAMED,
                FacilityStrategy.FACILITY_IGNORE, false),
                new OperationSearcher(name, argumentTypes));
    }

}