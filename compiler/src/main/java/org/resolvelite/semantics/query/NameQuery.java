package org.resolvelite.semantics.query;

import org.antlr.v4.runtime.Token;
import org.resolvelite.semantics.PossiblyQualifiedPath;
import org.resolvelite.semantics.searchers.NameSearcher;
import org.resolvelite.semantics.symbol.Symbol;
import org.resolvelite.semantics.SymbolTable.FacilityStrategy;
import org.resolvelite.semantics.SymbolTable.ImportStrategy;

/**
 * A {@code NameQuery} takes a (possibly-null) qualifier and a name
 * and searches for entries that match. If the qualifier is non-null, the
 * appropriate facility or module is searched. If it <em>is</em> null, a
 * search is performed using the provided {@code ImportStrategy} and
 * {@code FacilityStrategy}.</p>
 */
public class NameQuery extends BaseMultimatchSymbolQuery<Symbol>
        implements
            MultimatchSymbolQuery<Symbol> {

    public NameQuery(Token qualifier, String name,
            ImportStrategy importStrategy, FacilityStrategy facilityStrategy,
            boolean localPriority) {
        super(new PossiblyQualifiedPath(qualifier, importStrategy,
                facilityStrategy, localPriority), new NameSearcher(name, false));
    }

    public NameQuery(Token qualifier, Token name,
            ImportStrategy importStrategy, FacilityStrategy facilityStrategy,
            boolean localPriority) {
        this(qualifier, name.getText(), importStrategy, facilityStrategy,
                localPriority);
    }

    public NameQuery(Token qualifier, String name) {
        this(qualifier, name, ImportStrategy.IMPORT_NONE,
                FacilityStrategy.FACILITY_IGNORE, false);
    }

    public NameQuery(Token qualifier, Token name) {
        this(qualifier, name, ImportStrategy.IMPORT_NONE,
                FacilityStrategy.FACILITY_IGNORE, false);
    }
}