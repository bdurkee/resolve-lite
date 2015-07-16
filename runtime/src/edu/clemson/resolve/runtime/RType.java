package edu.clemson.resolve.runtime;

public interface RType {

    /**
     * Returns the Resolve representation of a programmatic type.
     * @return A {@link Object} containing pointing this types representation
     */
    Object getRep();

    void setRep(Object setRep);

    /**
     * All types in Resolve are given an initialzation value at in the modeling
     * phase. This initial value is implicitly applied when a variable is
     * declared to a Resolve {@code RType}.
     *
     * @return
     */
    RType initialValue();
    // boolean representationIsPrimitive();
}
