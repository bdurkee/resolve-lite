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
import org.resolvelite.compiler.tree.ImportCollection;
import org.resolvelite.misc.FileLocator;
import org.stringtemplate.v4.ST;
import org.resolvelite.compiler.AbstractCompilationPipeline;
import org.resolvelite.compiler.ResolveCompiler;
import org.resolvelite.compiler.tree.AnnotatedTree;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CodeGenPipeline extends AbstractCompilationPipeline {

    public CodeGenPipeline(@NotNull ResolveCompiler rc,
            @NotNull List<AnnotatedTree> compilationUnits) {
        super(rc, compilationUnits);
    }

    @Override public void process() {
        if ( compiler.genCode == null ) return;
        File outputDir = new File(compiler.outputDirectory);
        for (AnnotatedTree unit : compilationUnits) {
            try {
                CodeGenerator gen = new CodeGenerator(compiler, unit);
                compiler.info("generating code: " + unit.getName());
                if ( compiler.genCode.equals("Java") ) {
                    ST x = gen.generateModule();
                    System.out.println(x.render());
                    //gen.writeModuleFile(gen.generateModule());
                }
              /*  for (String external : unit.imports
                        .getImportsOfType(ImportCollection.ImportType.EXTERNAL)) {
                    FileLocator l =
                            new FileLocator(external,
                                    ResolveCompiler.NON_NATIVE_EXT);
                    Files.walkFileTree(
                            new File(compiler.libDirectory).toPath(), l);
                    File srcFile = l.getFile();
                    Path srcPath = l.getFile().toPath();
                    Path destPath =
                            new File(outputDir.getName() + "/"
                                    + srcFile.getName()).toPath();
                    Files.copy(srcPath, destPath,
                            StandardCopyOption.REPLACE_EXISTING);
                }*/
            }
            catch (IllegalStateException ise) {
                return; //if the templates were unable to be loaded, etc.
            }
           // catch (IOException ioe) {
          //      throw new RuntimeException(ioe.getMessage());
                //System.out.println(ioe.getMessage());
          //  }
        }
    }
}