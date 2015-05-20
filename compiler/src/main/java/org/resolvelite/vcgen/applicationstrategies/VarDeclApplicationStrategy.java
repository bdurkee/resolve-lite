package org.resolvelite.vcgen.applicationstrategies;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PDot;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.proving.absyn.PSymbol.PSymbolBuilder;
import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.MTType;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.programtype.PTGeneric;
import org.resolvelite.semantics.programtype.PTNamed;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.NameQuery;
import org.resolvelite.semantics.symbol.ProgTypeSymbol;
import org.resolvelite.typereasoning.TypeGraph;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock;

import java.util.Set;

public class VarDeclApplicationStrategy
        implements
            RuleApplicationStrategy<ResolveParser.VariableDeclGroupContext> {

    @Override public AssertiveCode applyRule(
            ResolveParser.VariableDeclGroupContext statement,
            VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        TypeGraph g = block.g;

        ProgTypeSymbol groupType =
                getProgType(block.scope, statement.type(),
                        statement.type().qualifier, statement.type().name);
        PExp finalConfirm = block.finalConfirm.getContents();

        for (TerminalNode term : statement.Identifier()) {

            if ( groupType.getProgramType() instanceof PTGeneric ) {
                block.assume(createInitializationPredicate(g, groupType, term));
            }
            else {
                PTNamed namedComponent = (PTNamed) groupType.getProgramType();
                PExp exemplar = createExemplarVariable(namedComponent);
                PExp variable =
                        new PSymbolBuilder(term.getText()).mathType(
                                groupType.getModelType()).build();
                PExp init =
                        block.annotations.getPExpFor(g,
                                namedComponent.getInitializationEnsures()
                                .mathAssertionExp())
                                .substitute(exemplar, variable);

                if ( finalConfirm.containsName(term.getText()) ) {
                    block.assume(init);
                 }
            }
        }
        return block.snapshot();
    }

    protected static ProgTypeSymbol getProgType(Scope s,
            ResolveParser.TypeContext type, Token qualifier, Token name) {
        try {
            return s.queryForOne(new NameQuery(type.qualifier, type.name, true))
                    .toProgTypeSymbol();
        }
        catch (NoSuchSymbolException | DuplicateSymbolException e) {
            throw new RuntimeException(e);//should find it --so noone cares
        }
    }

    protected static PSymbol createExemplarVariable(ProgTypeSymbol s) {
        return createExemplarVariable((PTNamed) s.getProgramType());
    }

    protected static PSymbol createExemplarVariable(PTNamed namedComponent) {
        return new PSymbol.PSymbolBuilder(namedComponent.getExemplarName())
                .mathType(namedComponent.toMath()).build();
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
