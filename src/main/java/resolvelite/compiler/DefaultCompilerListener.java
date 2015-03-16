/**
 * DefaultCompilerListener.java
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

import org.stringtemplate.v4.ST;

public class DefaultCompilerListener implements ResolveCompilerListener {

    private final ResolveCompiler compiler;

    public DefaultCompilerListener(ResolveCompiler c) {
        this.compiler = c;
    }

    @Override public void error(ResolveMessage msg) {
        ST msgST = compiler.errorManager.getMessageTemplate(msg);
        String outputMsg = msgST.render();
        if (compiler.errorManager.formatWantsSingleLineMessage()) {
            outputMsg = outputMsg.replace('\n', ' ');
        }
        System.err.println(outputMsg);
    }

    @Override public void info(String msg) {
        if (compiler.errorManager.formatWantsSingleLineMessage()) {
            msg = msg.replace('\n', ' ');
        }
        System.out.println(msg);
    }

    @Override public void warning(ResolveMessage msg) {
        ST msgST = compiler.errorManager.getMessageTemplate(msg);
        String outputMsg = msgST.render();
        if (compiler.errorManager.formatWantsSingleLineMessage()) {
            outputMsg = outputMsg.replace('\n', ' ');
        }
        System.err.println(outputMsg);
    }
}
