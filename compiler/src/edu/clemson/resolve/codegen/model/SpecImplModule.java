package edu.clemson.resolve.codegen.model;

/**
 * Forces implementation modules (specifically concept and enhancement impls)
 * to provide a constructor.
 */
public interface SpecImplModule {
    public void addCtor();
}