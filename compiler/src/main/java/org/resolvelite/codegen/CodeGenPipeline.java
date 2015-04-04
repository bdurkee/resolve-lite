/*
 * [The "BSD license"]
 * Copyright (c) 2015 Clemson University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 
 * 3. The name of the author may not be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.resolvelite.codegen;

import org.antlr.v4.runtime.misc.NotNull;
import org.resolvelite.misc.FileLocator;
import org.stringtemplate.v4.ST;
import org.resolvelite.compiler.AbstractCompilationPipeline;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    public CodeGenPipeline(@NotNull ResolveCompiler rc,
            @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        if ( compiler.genCode == null ) return;
        for (AnnotatedTree unit : compilationUnits) {
            compiler.info("generating code: " + unit.getName());
            CodeGenerator gen = new CodeGenerator(compiler, unit);
            if ( compiler.genCode.equals("Java") ) {
                ST x = gen.generateModule();
                //System.out.println(x.render());
                gen.writeModuleFile(gen.generateModule());
            }
        }

            //Todo: this process of copying externally realized stuff over from the
            //workspace is very basic atm. It doesn't take into account whether or
            //not an externally realized file is used in the context of any of the
            //current target files.
            try {
                FileLocator l = new FileLocator("java"); //Todo: Use non-native ext. in ResolveCompiler
                Files.walkFileTree(new File(compiler.libDirectory).toPath(), l);
                for (File externalFile : l.getFiles()) {
                    File out = new File(compiler.outputDirectory);
                    Path src = externalFile.toPath();
                    Path dest =
                            new File(out.getName() + "/" + externalFile.getName())
                                    .toPath();
                    Files.copy(externalFile.toPath(), dest,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            catch (IOException ioe) {
                throw new RuntimeException(ioe.getMessage());
                //System.out.println(ioe.getMessage());
            }
    }
}