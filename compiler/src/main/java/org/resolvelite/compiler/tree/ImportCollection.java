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
package org.resolvelite.compiler.tree;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.resolvelite.parsing.ResolveParser;

import java.util.*;

/**
 * Maintains a collection of module level imports. These imports are categorized
 * according to three groups:
 * <ul>
 * <li><strong>External</strong> imports arise from
 * {@link ResolveParser.FacilityDeclContext}s or enhancements whose specfied
 * implementations are preceded by the <tt>externally</tt> keyword.</li>
 * 
 * <li><strong>Implicit</strong> imports are those that are implicitly
 * referenced via a {@link ResolveParser.FacilityDeclContext} or enhancement.</li>
 * 
 * <li><strong>Explicit</strong> imports are any mentioned explicitly in the
 * <tt>imports</tt> list in a module's preamble.</li>
 * </ul>
 */
//Todo: This shit has gotten way too confusing. Delete this, simplify it, whatever it takes.
//and make it so the importListener doesn't crash -- it's run before we do our filtering step for
//modules with gross syntactic errors.
public class ImportCollection {

    public static enum ImportType {
        NAMED, IMPLICIT, EXTERNAL
    }

    private final Map<ImportType, LinkedHashSet<String>> imports =
            new HashMap<>();

    public ImportCollection() {
        for (int i = 0; i < ImportType.values().length; i++) {
            ImportType curType = ImportType.values()[i];
            if ( imports.get(curType) == null ) {
                imports.put(curType, new LinkedHashSet<>());
            }
        }
    }

    /**
     * Retrieves a set of all imports except those of <code>type</code>.
     * 
     * @param type Any types we would like to filter/exclude.
     * @return A set of imports filtered by <code>type</code>.
     */
    public Set<String> getImportsExcluding(ImportType... type) {
        Set<String> result = new HashSet<>();
        List<ImportType> typesToExclude = Arrays.asList(type);

        for (ImportType s : imports.keySet()) {
            if ( !typesToExclude.contains(s) ) {
                result.addAll(imports.get(s));
            }
        }
        return result;
    }

    public List<String> getImportsOfType(ImportType type) {
        return new ArrayList<>(imports.get(type));
    }

    public void imports(ImportType type, String... t) {
        addTokenSet(type, Arrays.asList(t));
    }

    public void imports(ImportType type, TerminalNode... t) {
        imports(type, Arrays.asList(t));
    }

    public void imports(ImportType type, List<TerminalNode> terminals) {
        List<String> convertedTerms = new ArrayList<>();
        for (TerminalNode t : terminals) {
            convertedTerms.add(t.getSymbol().getText());
        }
        addTokenSet(type, convertedTerms);
    }

    public void addImports(ImportType type, String... t) {
        addTokenSet(type, Arrays.asList(t));
    }

    public void addTokenSet(ImportType type, Collection<String> newToks) {
        LinkedHashSet<String> tokSet = imports.get(type);
        if ( tokSet == null ) {
            tokSet = new LinkedHashSet<String>();
        }
        //Todo: Do a little normalization here on additions. For instance,
        //if something already exists in the map as an implicit import,
        //but is later added as an explicit import too, then it the import
        //should appear twice in two different categories (especially if
        // it's already listed as an implicit import). So in other words,
        //cull duplicate references in multiple categories...
        tokSet.addAll(newToks);
    }

    /**
     * Returns <code>true</code> <strong>iff</strong> the set of
     * <code>type</code> imports contains <code>t</code>; <code>false</code>
     * otherwise.
     * 
     * @param type A {@link ImportType}.
     * @param t A name token.
     * 
     * @return <code>true</code> if <code>t</code> is in the set of
     *         <code>type</code>, <code>false</code> otherwise.
     */
    public boolean inCategory(ImportType type, String t) {
        return imports.get(type).contains(t);
    }

    public Set<String> getAllImports() {
        Set<String> aggregateImports = new HashSet<>();

        for (Set<String> typeSet : imports.values()) {
            aggregateImports.addAll(typeSet);
        }
        return aggregateImports;
    }

}
