package org.resolvelite.codegen.model;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates a field of an {@link OutputModelObject} that should be walked when
 * constructing a hierarchy of {@link org.stringtemplate.v4.ST}s.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ModelElement {}