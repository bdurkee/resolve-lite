/**
 * BindingException.java
 * ---------------------------------
 * Copyright (c) 2015
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package org.rsrg.semantics;

public class BindingException extends Exception {

    private static final long serialVersionUID = 1L;

    public final MathType found, expected;

    public BindingException(MathType found, MathType expected) {
        this.found = found;
        this.expected = expected;
    }
}
