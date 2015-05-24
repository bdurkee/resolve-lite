package org.resolvelite.vcgen.applicationstrategies;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PDot;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.programtype.PTGeneric;
import org.resolvelite.semantics.programtype.PTNamed;
import org.resolvelite.semantics.symbol.ProgTypeSymbol;
import org.resolvelite.typereasoning.TypeGraph;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock;

import static org.resolvelite.vcgen.applicationstrategies.VarDeclApplicationStrategy.createExemplarVariable;

/**
 * Applies the variable decl rule to a group of variable declarations in a
 * record.
 */
public class RecordVarDeclApplicationStrategy
        implements
            RuleApplicationStrategy<ResolveParser.RecordVariableDeclGroupContext> {

    @Override public AssertiveCode applyRule(
            ResolveParser.RecordVariableDeclGroupContext statement,
            VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        TypeGraph g = block.g;
        ProgTypeSymbol typeSym =
                VarDeclApplicationStrategy.getProgType(block.scope,
                        statement.type(), statement.type().qualifier,
                        statement.type().name);
        PExp finalConfirm = block.finalConfirm.getContents();

        for (TerminalNode term : statement.Identifier()) {
            if ( typeSym.getProgramType() instanceof PTGeneric ) {
                block.assume(VarDeclApplicationStrategy
                        .createInitializationPredicate(g, typeSym, term));
            }
            else {
                PTNamed namedComponent = (PTNamed) typeSym.getProgramType();
                PSymbol exemplar = createExemplarVariable(namedComponent);
                PSymbol variable =
                        new PSymbol.PSymbolBuilder(term.getText()).mathType(
                                typeSym.getModelType()).build();

                PExp dot =
                        new PDot(typeSym.getModelType(), null, exemplar,
                                variable);
                PExp init =
                        namedComponent.getInitializationEnsures().substitute(
                                exemplar, dot);

                if ( finalConfirm.containsName(term.getText()) ) {
                    block.assume(init);
                }
            }
        }
        return block.snapshot();
    }

    @Override public String getDescription() {
        return "record variable decl rule application";
    }
}
