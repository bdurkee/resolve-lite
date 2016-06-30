package edu.clemson.resolve.semantics;

/**
 * A general representation of mathematical quantification.
 * <p>
 * This enum should be used for anything that is capable of being quantified (e.g. like {@link MathClssftn}s or
 * {@link edu.clemson.resolve.proving.absyn.PExp}s).
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