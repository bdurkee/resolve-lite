package org.resolvelite.vcgen.applicationstrategies;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PDot;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.programtype.PTGeneric;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.symbol.ProgTypeSymbol;
import org.resolvelite.typereasoning.TypeGraph;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock;

public class VarDeclarationApplicationStrategy
        implements
            RuleApplicationStrategy<ResolveParser.VariableDeclGroupContext> {

    @Override public AssertiveCode applyRule(
            ResolveParser.VariableDeclGroupContext statement,
            VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        TypeGraph g = block.g;
        ResolveParser.TypeContext type = statement.type();
        ProgTypeSymbol typeSym = null;
        try {
            typeSym =
                    block.scope.queryForOne(
                            new NameQuery(type.qualifier, type.name, true))
                            .toProgTypeSymbol();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            throw new RuntimeException(e);//should find it --so noone cares
        }

        for (TerminalNode term : statement.Identifier()) {
            if ( typeSym.getProgramType() instanceof PTGeneric ) {
                block.assume(createInitializationPredicate(g, typeSym, term));
            }
            else {
                throw new UnsupportedOperationException(
                        "normally typed var decls "
                                + "not yet supported by vcgen");
            }
        }

        return block.snapshot();
    }

    protected static PDot createInitializationPredicate(TypeGraph g,
            ProgTypeSymbol type, TerminalNode t) {
        PSymbol predicateArg =
                new PSymbolBuilder(t.getText()).mathType(type.getModelType())
                        .build();
        PSymbol predicate =
                new PSymbolBuilder("Is_Initial").mathType(g.BOOLEAN)
                        .arguments(predicateArg).build();
        return new PDot(g.BOOLEAN, null, type.asPSymbol(), predicate);
    }

    @Override public String getDescription() {
        return "variable decl rule application";
    }
}
