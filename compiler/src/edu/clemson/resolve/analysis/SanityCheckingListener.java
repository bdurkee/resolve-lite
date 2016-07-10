package edu.clemson.resolve.analysis;

import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.parser.ResolveParser;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.misc.Predicate;
import org.jetbrains.annotations.NotNull;

/**
 * Uses a combination of listeners and visitors to check for some semantic
 * errors uncheckable in the grammar and ommited by {@link PopulatingVisitor}.
 * <p>
 * To make many of our checks here easier, we make sure to have already built
 * the ast for exprs in a previous phase.</p>
 */
public class SanityCheckingListener extends ResolveBaseListener {

    private final RESOLVECompiler compiler;
    private final AnnotatedModule tr;

    public SanityCheckingListener(@NotNull RESOLVECompiler compiler,
                                  @NotNull AnnotatedModule tr) {
        this.compiler = compiler;
        this.tr = tr;
    }

    /**
     * Decends into every node in an arbitrary parsetree and listens for
     * {@link ResolveParser.ProgParamExpContext}s whose characteristics satisfy
     * some arbitrary {@link Predicate}.
     * <p>
     * After a listen, you can check to see if at any point the predicate was
     * satisfied via the public {@link #result} member. If you need more
     * specific information, such as "which contexts satisfied my predicate?",
     * refer to {@link #satisfyingContexts} for a complete list.</p>
     */
    private static class CallCheckingListener
            extends
            ResolveBaseListener {

        /** Some {@link Predicate} that operates on {@link ResolveParser.ProgParamExpContext}s */
     /*   private final Predicate<ResolveParser.ProgParamExpContext> checker;

        public boolean result = false;
        List<ResolveParser.ProgParamExpContext> satisfyingContexts = new ArrayList<>();

        CallCheckingListener(
                @NotNull Predicate<ResolveParser.ProgParamExpContext> checker) {
            this.checker = checker;
        }
        @Override public void exitProgParamExp(
                ResolveParser.ProgParamExpContext ctx) {
            if (checker.test(ctx)) {
                result = true;
                satisfyingContexts.add(ctx);
            }
        }*/
    }
}
