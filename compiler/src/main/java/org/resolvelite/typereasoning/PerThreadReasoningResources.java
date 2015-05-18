package org.resolvelite.typereasoning;

import org.resolvelite.semantics.AlphaEquivalencyChecker;

/**
 * Type reasoning is used extensively by the prover, where things are done
 * in a tight loop that needs to run as quickly as possible. As a result,
 * performance is at a premium and we want to avoid dynamic object creation.
 * At the same time, we can't have a bunch of static variables running around
 * because many of these structures are not thread safe. This class is
 * guaranteed not to be shared between threads.
 */
public class PerThreadReasoningResources {

    public final AlphaEquivalencyChecker alphaChecker =
            new AlphaEquivalencyChecker();
}