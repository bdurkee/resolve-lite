package edu.clemson.resolve.runtime;

/**
 * A default operation-like parameter that filters all extraneous RType methods, while providing a
 * dummy implementation of the primary {@link BaseOperationParameter#op} method.
 */
public class BaseOperationParameter implements OperationParameter {

    @Override public RType op(RType... e) {
        return null;
    }

    @Override public final Object getRep() {
        throw new UnsupportedOperationException("getRep() shouldn't be "
                + "getting called from: " + this.getClass());
    }

    @Override public final void setRep(Object setRep) {
        throw new UnsupportedOperationException("setRep() shouldn't be "
                + "getting called from: " + this.getClass());
    }

    @Override public final RType initialValue() {
        throw new UnsupportedOperationException("initialValue() shouldn't"
                + " be getting called from: " + this.getClass());
    }
}
