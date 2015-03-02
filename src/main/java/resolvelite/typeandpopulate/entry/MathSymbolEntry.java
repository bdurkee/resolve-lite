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
package resolvelite.typeandpopulate.entry;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.ResolveCompiler;
import resolvelite.typeandpopulate.MTProper;
import resolvelite.typeandpopulate.MTType;
import resolvelite.typeandpopulate.ModuleIdentifier;
import resolvelite.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;

public class MathSymbolEntry extends SymbolTableEntry {

    private final MTType type;
    private final MTType typeValue;
    private final Quantification quantification;

    /**
     * <p>Math symbols that represent definitions can take parameters, which may
     * contain implicit type parameters that cause the definition's true type
     * to change based on the type of arguments that end up actually passed.
     * These parameters are represented in this map, with the key giving the
     * name of the type parameter (which will then behave as a normal, bound,
     * named type within the definition's type) and the value giving the type
     * bounds of the parameter.</p>
     */
    private final Map<String, MTType> mySchematicTypes =
            new HashMap<String, MTType>();

    public MathSymbolEntry(ResolveCompiler compiler,
                           TypeGraph g, String name, Quantification q,
                           ParseTree definingElement, MTType type, MTType typeValue,
                           Map<String, MTType> schematicTypes) {
        super(compiler, name, definingElement);

        if (schematicTypes != null) {
            mySchematicTypes.putAll(schematicTypes);
        }

        this.type = type;
        this.quantification = q;
        if (typeValue != null) {
            this.typeValue = typeValue;
        }
        else if (type.isKnownToContainOnlyMTypes()) {
            this.typeValue =
                    new MTProper(g, type, type
                            .membersKnownToContainOnlyMTypes(), name);
        }
        else {
            this.typeValue = null;
        }
    }

    @NotNull
    public Quantification getQuantification() {
        return quantification;
    }

    @Override
    public String toString() {
        return getName() + "\t\t"
                + quantification + "\t\tOf type: " + type
                + "\t\t Defines type: " + typeValue;
    }

    @Override
    public MathSymbolEntry toMathSymbolEntry(Token l) {
        return this;
    }

    @Override
    public String getEntryTypeDescription() {
        return "a math symbol";
    }
}
