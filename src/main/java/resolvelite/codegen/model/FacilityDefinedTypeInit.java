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
package resolvelite.codegen.model;

import resolvelite.semantics.symbol.FacilitySymbol;

public class FacilityDefinedTypeInit extends TypeInit {
    //if we represent a literal initialization, we pass the initial value as
    //a str to the createX(..) method.
    @ModelElement public FacilityQualifier qualifier;

    public FacilityDefinedTypeInit(FacilityQualifier typeFacilityQualifier,
            String typeToInitName) {
        this(typeFacilityQualifier, typeToInitName, "");
    }

    public FacilityDefinedTypeInit(FacilityQualifier typeFacilityQualifier,
            String typeToInitName, String initialValue) {
        super(typeToInitName, initialValue);
        this.qualifier = typeFacilityQualifier;
    }

    public static class FacilityQualifier extends OutputModelObject {
        public String facilitySpecName, facilityName;

        public FacilityQualifier(FacilitySymbol f) {
            this(f.getName(), f.getSpecName());
        }

        public FacilityQualifier(String facilityName, String facilitySpecName) {
            this.facilityName = facilityName;
            this.facilitySpecName = facilitySpecName;
        }
    }
}
