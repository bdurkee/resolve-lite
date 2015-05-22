package org.resolvelite.vcgen.applicationstrategies;

import org.antlr.v4.runtime.ParserRuleContext;
import org.resolvelite.compiler.tree.AnnotatedTree;
import org.resolvelite.parsing.ResolveParser;
import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.proving.absyn.PSymbol;
import org.resolvelite.semantics.DuplicateSymbolException;
import org.resolvelite.semantics.NoSuchSymbolException;
import org.resolvelite.semantics.Scope;
import org.resolvelite.semantics.programtype.PTType;
import org.resolvelite.semantics.query.OperationQuery;
import org.resolvelite.semantics.symbol.OperationSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol.ParameterMode;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.*;
import java.util.stream.Collectors;

public class ExplicitCallApplicationStrategy<E extends ParserRuleContext>
        implements
            RuleApplicationStrategy<E> {

    @Override public AssertiveCode applyRule(
            E call, VCAssertiveBlock.VCAssertiveBlockBuilder block) {
        PSymbol asPSym;
        if (call.getClass().equals(ResolveParser.CallStmtContext.class)) {
            asPSym = (PSymbol)block.annotations.getPExpFor(block.g,
                    (ParserRuleContext)call.getChild(0));
        }
        else {
            asPSym = (PSymbol)block.annotations.getPExpFor(block.g, call);
        }
        AnnotatedTree annotations = block.annotations;

        OperationSymbol op = getOperation(block.scope, asPSym);

        List<PExp> actuals = asPSym.getArguments();
        List<PExp> formals = op.getParameters().stream()
                .map(ProgParameterSymbol::asPSymbol).collect(Collectors.toList());
        /**
         * So: {@pre Oper op (x: T): U; pre /_x_\; post op = f/_x_\} is in Ctx
         * and our statement reads as follows: {@code v := op(u);}. Informally
         * this next line substitutes appearances of the formal parameter
         * {@code x} in op's requires clause with the actuals (more formally,
         * {@code pre[x -> u]}).
         */
        PExp opRequires = annotations.getPExpFor(block.g, op.getRequires());
        block.confirm(opRequires.substitute(formals, actuals));

        PSymbol opEnsures = (PSymbol)annotations
                .getPExpFor(block.g, op.getEnsures());
        if (opEnsures.isLiteralTrue()) return block.snapshot();

        List<PExp> con = block.finalConfirm.getContents().splitIntoConjuncts();

        PExp ensuresLeft = opEnsures.getArguments().get(0);
        PExp ensuresRight = opEnsures.getArguments().get(1);

        //update our list of formal params to account for incoming-valued refs
        //to themselves in the ensures clause
        for (PSymbol f : ensuresRight.getIncomingVariables()) {
            Collections.replaceAll(formals, f.withIncomingSignsErased(), f);
        }

        /**
         * Now we substitute the formals for actuals in the rhs of the ensures
         * ({@code f}), THEN replace all occurences of {@code v} in {@code Q}
         * with the modified {@code f} (formally, {@code Q[v -> f[x -> u]]}).
         */
        ensuresRight = ensuresRight.substitute(formals, actuals);
        Map<String, ProgParameterSymbol.ParameterMode> modes =
                new LinkedHashMap<>();

        //The collection of 'updates' actuals that should be replaced in the
        //existing final confirm.
        List<PExp> replacementActuals = op.getParameters().stream()
                .filter(p -> p.getMode() == ParameterMode.UPDATES)
                .map(ProgParameterSymbol::asPSymbol)
                .collect(Collectors.toList());

        block.finalConfirm(block.finalConfirm.getContents()
                .substitute(replacementActuals, ensuresRight));
        return block.snapshot();
    }

    protected static OperationSymbol getOperation(Scope s, PSymbol app) {
        List<PTType> argTypes = app.getArguments().stream()
                .map(PExp::getProgType).collect(Collectors.toList());
        try {
            return s.queryForOne(new OperationQuery(app.getQualifier(),
                    app.getName(), argTypes));
        }
        catch (NoSuchSymbolException|DuplicateSymbolException e) {
            //shouldn't happen. Well, depends on s.
            throw new RuntimeException(e);
        }
    }

    @Override public String getDescription() {
        return "explicit (simple) call rule application";
    }
}
