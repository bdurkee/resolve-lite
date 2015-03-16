/**
 * LanguageSemanticsMessage.java
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
import resolvelite.misc.Utils;

public class LanguageSemanticsMessage extends ResolveMessage {

    public LanguageSemanticsMessage(ErrorKind etype, Token offendingToken,
            Object... args) {
        super(etype, offendingToken, args);
        if (offendingToken != null) {
            this.fileName =
                    Utils.groomFileName(offendingToken.getTokenSource()
                            .getSourceName());
            this.line = offendingToken.getLine();
            this.charPosition = offendingToken.getCharPositionInLine();
        }
    }
}
