/**
 * CompilerMessage.java
 * ---------------------------------
 * Copyright (c) 2014
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package resolvelite.compiler;

/**
 * 
 * A generic message from the tool such as "file not found" type errors; there
 * is no reason to create a special object for each error unlike the grammar
 * errors, which may be rather complex.
 * <p>
 * Sometimes you need to pass in a filename or something to say it is "bad".
 * Allow a generic object to be passed in and the string template can deal with
 * just printing it or pulling a property out of it.
 * </p>
 */
public class CompilerMessage extends ResolveMessage {

    public CompilerMessage(ErrorKind errorType) {
        super(errorType);
    }

    public CompilerMessage(ErrorKind errorType, Object... args) {
        super(errorType, null, null, args);
    }

    public CompilerMessage(ErrorKind errorType, Throwable e, Object... args) {
        super(errorType, e, null, args);
    }
}