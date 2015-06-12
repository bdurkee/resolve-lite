package org.resolvelite.semantics;

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