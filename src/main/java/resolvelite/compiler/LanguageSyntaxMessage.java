/**
 * LanguageSyntaxMessage.java
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

import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

public class LanguageSyntaxMessage extends ResolveMessage {

    public LanguageSyntaxMessage(ErrorKind etype, Token offendingToken,
            RecognitionException antlrException, Object... args) {
        super(etype, antlrException, offendingToken, args);
        this.offendingToken = offendingToken;
        if (offendingToken != null) {
            this.fileName = offendingToken.getTokenSource().getSourceName();
            this.line = offendingToken.getLine();
            this.charPosition = offendingToken.getCharPositionInLine();
        }
    }

    @SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" }) @Override public
            RecognitionException getCause() {
        return (RecognitionException) super.getCause();
    }
}
