package edu.clemson.resolve.runtime;

/**
 * This class provided to allow non-RESOLVE objects to interact with RESOLVE
 * objects.
 *
 * @author H. Smith <hsmith@g.clemson.edu>
 */
public class RTypeWrapper implements RType {
    private Object contents;

    public RTypeWrapper(Object o) {
        this.contents = o;
    }

    public Object getRep() {
        return contents;
    }

    public void setRep(Object setRep) {
        this.contents = setRep;
    }

    public RType initialValue() {
        return new RTypeWrapper("(an empty RTypeWrapper)");
    }
}