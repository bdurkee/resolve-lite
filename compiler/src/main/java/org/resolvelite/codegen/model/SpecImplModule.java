package org.resolvelite.codegen.model;

/**
 * Forces implementation modules (specifically concept and enhancement impls)
 * to implement a Ctor method.
 */
public interface SpecImplModule {

    public void addCtor();
}