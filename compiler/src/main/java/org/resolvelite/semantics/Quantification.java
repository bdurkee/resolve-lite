package org.resolvelite.semantics;

public enum Quantification {

    NONE {

        protected Quantification flipped() {
            return NONE;
        }
    },
    UNIVERSAL {

        protected Quantification flipped() {
            return EXISTENTIAL;
        }
    },
    EXISTENTIAL {

        protected Quantification flipped() {
            return UNIVERSAL;
        }
    };

    protected abstract Quantification flipped();
}