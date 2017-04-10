package edu.clemson.resolve.compiler;

/**
 * A generic message from the tool such as "file not found" type errors; there is no reason to create a special object
 * for each error unlike those that might arise from user sourcecode, which may be rather complex.
 * <p>
 * Sometimes you need to pass in a filename or something to say it is "bad". Allow a generic {@code Object} to be
 * passed in and the template in the view can deal with just printing it or pulling some property out of it.</p>
 */
public class CompilerMessage extends RESOLVEMessage {

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