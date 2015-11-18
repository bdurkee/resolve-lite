package org.rsrg.semantics.query;

import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.PossiblyQualifiedPath;
import org.rsrg.semantics.MathSymbolTableBuilder.FacilityStrategy;
import org.rsrg.semantics.MathSymbolTableBuilder.ImportStrategy;
import org.rsrg.semantics.searchers.NameSearcher;
import org.rsrg.semantics.symbol.Symbol;

/**
 * A {@code NameQuery} takes a (possibly-null) qualifier and a name
 * and searches for entries that match. If the qualifier is non-null, the
 * appropriate facility or module is searched. If it <em>is</em> null, a
 * search is performed using the provided {@code ImportStrategy} and
 * {@code FacilityStrategy}.
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

    public NameQuery(Token qualifier, Token name, boolean localPriority) {
        this(qualifier, name.getText(), ImportStrategy.IMPORT_NAMED,
                FacilityStrategy.FACILITY_IGNORE, localPriority);
    }
}