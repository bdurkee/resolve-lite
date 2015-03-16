/**
 * ResolveMessage.java
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

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.misc.Nullable;
import org.stringtemplate.v4.ST;

import java.util.Arrays;

public class ResolveMessage {

    private static final Object[] EMPTY_ARGS = new Object[0];

    @NotNull private final ErrorKind errorType;
    @Nullable private final Object[] args;
    @Nullable private final Throwable e;

    // used for location template
    public String fileName;
    public int line = -1;
    public int charPosition = -1;
    public Token offendingToken;

    public ResolveMessage(@NotNull ErrorKind errorKind) {
        this(errorKind, (Throwable) null, null);
    }

    public ResolveMessage(@NotNull ErrorKind errorType, Token offendingToken,
            Object... args) {
        this(errorType, null, offendingToken, args);
    }

    public ResolveMessage(@NotNull ErrorKind errorType, @Nullable Throwable e,
            Token offendingToken, Object... args) {
        this.errorType = errorType;
        this.e = e;
        this.args = args;
        this.offendingToken = offendingToken;
    }

    @NotNull public ErrorKind getErrorType() {
        return errorType;
    }

    @NotNull public Object[] getArgs() {
        if (args == null) {
            return EMPTY_ARGS;
        }
        return args;
    }

    public ST getMessageTemplate(boolean verbose) {
        ST messageST = new ST(getErrorType().message);
        messageST.impl.name = errorType.name();

        messageST.add("verbose", verbose);
        Object[] args = getArgs();
        for (int i = 0; i < args.length; i++) {
            String attr = "arg";
            if (i > 0) {
                attr += i + 1;
            }
            messageST.add(attr, args[i]);
        }
        if (args.length < 2) {
            messageST.add("arg2", null);
        }
        Throwable cause = getCause();
        if (cause != null) {
            messageST.add("exception", cause);
            messageST.add("stackTrace", cause.getStackTrace());
        }
        else {
            messageST.add("exception", null); // avoid ST error msg
            messageST.add("stackTrace", null);
        }
        return messageST;
    }

    @Nullable public Throwable getCause() {
        return e;
    }

    @Override public String toString() {
        return "Message{" + "errorType=" + getErrorType() + ", args="
                + Arrays.asList(getArgs()) + ", e=" + getCause()
                + ", fileName='" + fileName + '\'' + ", line=" + line
                + ", charPosition=" + charPosition + '}';
    }
}
