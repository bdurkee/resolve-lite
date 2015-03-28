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
package resolvelite.compiler.tree;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.TerminalNode;
import resolvelite.misc.Utils.Builder;

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
 * referenced via a {@link ResolveParser.FacilityDecl} or enhancement.</li>
 * 
 * <li><strong>Explicit</strong> imports are any mentioned explicitly in the
 * <tt>imports</tt> list in a module's preamble.</li>
 * </ul>
 */
//Todo: If an import is classified as explicit then a facility implicitly
//makes mention of it again, delete it from the implicit list (since it's
//actually now explicit. In other words, explicit imports in some sense have
//highest priority; so if it's listed there, no need to mention it again
//implicitly.
public class ImportCollection {

    public static enum ImportType {
        EXPLICIT, IMPLICIT, EXTERNAL
    }

    private final Map<ImportType, Set<Token>> imports;

    private ImportCollection(ImportCollectionBuilder builder) {
        this.imports = builder.imports;
    }

    /**
     * Retrieves a set of all imports except those of <code>type</code>.
     * 
     * @param type Any types we would like to filter/exclude.
     * @return A set of {@link Token} filtered by <code>type</code>.
     */
    public Set<Token> getImportsExcluding(ImportType... type) {
        Set<Token> result = new HashSet<>();
        List<ImportType> typesToExclude = Arrays.asList(type);

        for (ImportType s : imports.keySet()) {
            if ( !typesToExclude.contains(s) ) {
                result.addAll(imports.get(s));
            }
        }
        return result;
    }

    public Set<Token> getImportsOfType(ImportType type) {
        return imports.get(type);
    }

    /**
     * Removes {@link Token} <code>t</code> from this import collection.
     * 
     * @param e The token reference to remove.
     * @return A modified collection with <code>e</code> deleted.
     */
    public void removeImport(Token e) {
        for (Set<Token> category : imports.values()) {
            category.remove(e);
        }
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
    public boolean inCategory(ImportType type, Token t) {
        return imports.get(type).contains(t);
    }

    /**
     * <p>
     * Returns all imports, regardless of their <code>ImportType</code>, in a
     * single set.
     * </p>
     * 
     * @return <strong>All</strong> imports.
     */
    public Set<Token> getAllImports() {
        Set<Token> aggregateImports = new HashSet<Token>();

        for (Set<Token> typeSet : imports.values()) {
            aggregateImports.addAll(typeSet);
        }
        return aggregateImports;
    }

    public static class ImportCollectionBuilder
            implements
                Builder<ImportCollection> {

        protected final Map<ImportType, Set<Token>> imports =
                new HashMap<ImportType, Set<Token>>();

        public ImportCollectionBuilder() {
            this(null, null);
        }

        public ImportCollectionBuilder(Token start, Token stop) {
            //Initialize the uses/import map to empty sets
            for (int i = 0; i < ImportType.values().length; i++) {
                ImportType curType = ImportType.values()[i];
                if ( imports.get(curType) == null ) {
                    imports.put(curType, new HashSet<Token>());
                }
            }
        }

        /*
         * public ImportCollectionBuilder imports(
         * 
         * @NotNull ResolveParser.FacilityDeclContext ctx) {
         * imports(ImportType.IMPLICIT, ctx.concept);
         * ImportType specType =
         * (ctx.externally != null) ? ImportType.EXTERNAL
         * : ImportType.IMPLICIT;
         * imports(specType, ctx.impl);
         * //Todo: External keyword for enhancement pairs.
         * for (ResolveParser.EnhancementPairDeclContext enhancement : ctx
         * .enhancementPairDecl()) {
         * imports(ImportType.IMPLICIT, enhancement.spec);
         * imports(specType, enhancement.impl);
         * }
         * return this;
         * }
         */

        public ImportCollectionBuilder imports(ImportType type, Token... t) {
            addTokenSet(type, Arrays.asList(t));
            return this;
        }

        public ImportCollectionBuilder imports(ImportType type,
                TerminalNode... t) {
            imports(type, Arrays.asList(t));
            return this;
        }

        public ImportCollectionBuilder imports(ImportType type,
                List<TerminalNode> terminals) {
            List<Token> convertedTerms = new ArrayList<Token>();
            for (TerminalNode t : terminals) {
                convertedTerms.add(t.getSymbol());
            }
            addTokenSet(type, convertedTerms);
            return this;
        }

        public ImportCollectionBuilder addImports(ImportType type, Token... t) {
            addTokenSet(type, Arrays.asList(t));
            return this;
        }

        private void addTokenSet(ImportType type,
                Collection<? extends Token> newToks) {
            Set<Token> tokSet = imports.get(type);
            if ( tokSet == null ) {
                tokSet = new HashSet<Token>();
            }
            //Todo: Do a little normalization here on additions. For instance,
            //if something already exists in the map as an implicit import,
            //but is later added as an explicit import too, then it the import
            //should appear twice in two different categories (especially if
            // it's already listed as an implicit import). So in other words,
            //cull duplicate references in multiple categories...
            tokSet.addAll(newToks);
        }

        @Override public ImportCollection build() {
            return new ImportCollection(this);
        }
    }

}
