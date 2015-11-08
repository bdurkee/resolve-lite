package org.rsrg.semantics;

/**
 * A general, reusable representation of mathematical quantification.
 * This enum should be used for anything that is capable of being quantified
 * (e.g. {@link edu.clemson.resolve.proving.absyn.PExp}s).
 */
public enum Quantification {

    NONE {

        public Quantification flipped() {
            return NONE;
        }
    },
    UNIVERSAL {

        public Quantification flipped() {
            return EXISTENTIAL;
        }
    },
    EXISTENTIAL {

        public Quantification flipped() {
            return UNIVERSAL;
        }
    };

    public abstract Quantification flipped();
}