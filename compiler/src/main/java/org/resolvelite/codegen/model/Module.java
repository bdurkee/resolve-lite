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
package org.resolvelite.codegen.model;

import org.resolvelite.semantics.symbol.GenericSymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Module extends OutputModelObject {
    public String name;
    public ModuleFile file;
    @ModelElement public List<FunctionImpl> funcImpls = new ArrayList<>();
    @ModelElement public List<MemberClassDef> repClasses = new ArrayList<>();
    @ModelElement public List<VariableDef> memberVars = new ArrayList<>();

    public Module(String name, ModuleFile file) {
        this.name = name;
        this.file = file;//who contains us?
    }

    /**
     * Like the name suggests, adds getters and member variabes for the formal
     * parameters to a concept (or enhancement).
     */
    public abstract void addGetterMethodsAndVarsForConceptualParamsAndGenerics(
            List<? extends Symbol> symbols);

    /**
     * For implementations that take an operation as a parameter, this method
     * adds both an RType member variable pointing to the interface wrapping
     * the 'operation' as well as the interior interfaces wrapping calls to
     * the operation.
     * 
     * @param wrappedFunction
     */
    public abstract void addOperationParameterModelObjects(
            FunctionDef wrappedFunction);
}