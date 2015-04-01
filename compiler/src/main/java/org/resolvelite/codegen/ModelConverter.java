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

import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.STGroup;
import org.stringtemplate.v4.compiler.FormalArgument;
import org.resolvelite.codegen.model.ModelElement;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.compiler.ErrorKind;
import org.resolvelite.compiler.ResolveCompiler;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Convert an output model tree to template hierarchy by walking
 * the output model. Each output model object has a corresponding template
 * of the same name. An output model object can have nested objects.
 * We identify those nested objects by the list of arguments in the template
 * definition. For example, here is the definition of the parser template:
 * 
 * <pre>
 * Parser(parser, scopes, funcs) ::= <<...>>
 * </pre>
 * <p>
 * The first template argument is always the output model object from which this
 * walker will create the template. Any other arguments identify the field names
 * within the output model object of nested model objects. So, in this case,
 * template Parser is saying that output model object Parser has two fields the
 * walker should chase called scopes and funcs.
 * <p>
 * This simple mechanism means we don't have to include code in every output
 * model object that says how to create the corresponding template.
 * 
 * @author TParr <parrt@cs.usfca.edu>
 */
public class ModelConverter {

    private final ResolveCompiler compiler;
    private final STGroup templates;

    public ModelConverter(ResolveCompiler rc, STGroup templates) {
        this.compiler = rc;
        this.templates = templates;
    }

    public ST walk(OutputModelObject omo) {
        // CREATE TEMPLATE FOR THIS OUTPUT OBJECT
        Class<? extends OutputModelObject> cl = omo.getClass();
        String templateName = cl.getSimpleName();
        ST st = templates.getInstanceOf(templateName);
        if ( st == null ) {
            compiler.errorManager.toolError(
                    ErrorKind.CODE_GEN_TEMPLATES_INCOMPLETE, templateName);
            return new ST("[" + templateName + " invalid]");
        }
        if ( st.impl.formalArguments == null ) {
            compiler.errorManager.toolError(ErrorKind.CODE_TEMPLATE_ARG_ISSUE,
                    templateName, "<none>");
            return st;
        }

        Map<String, FormalArgument> formalArgs = st.impl.formalArguments;

        // PASS IN OUTPUT MODEL OBJECT TO TEMPLATE AS FIRST ARG
        Set<String> argNames = formalArgs.keySet();
        Iterator<String> arg_it = argNames.iterator();
        String modelArgName = arg_it.next(); // ordered so this is first arg
        st.add(modelArgName, omo);

        // COMPUTE STs FOR EACH NESTED MODEL OBJECT MARKED WITH
        // @ModelElement AND MAKE ST ATTRIBUTE
        Set<String> usedFieldNames = new HashSet<>();
        Field fields[] = cl.getFields();
        for (Field fi : fields) {
            ModelElement annotation = fi.getAnnotation(ModelElement.class);
            if ( annotation == null ) {
                continue;
            }

            String fieldName = fi.getName();

            if ( !usedFieldNames.add(fieldName) ) {
                compiler.errorManager.toolError(ErrorKind.INTERNAL_ERROR,
                        "Model object " + omo.getClass().getSimpleName()
                                + " has multiple fields named '" + fieldName
                                + "'");
                continue;
            }

            // Just don't set @ModelElement fields w/o formal arg in target ST
            if ( formalArgs.get(fieldName) == null ) {
                continue;
            }
            try {
                Object o = fi.get(omo);
                if ( o instanceof OutputModelObject ) { // SINGLE MODEL OBJECT?
                    OutputModelObject nestedOmo = (OutputModelObject) o;
                    ST nestedST = walk(nestedOmo);
                    //System.out.println("set ModelElement "+fieldName+"="+nestedST+" in "+templateName);
                    st.add(fieldName, nestedST);
                }
                else if ( o instanceof Collection
                        || o instanceof OutputModelObject[] ) {
                    // LIST OF MODEL OBJECTS?
                    if ( o instanceof OutputModelObject[] ) {
                        o = Arrays.asList((OutputModelObject[]) o);
                    }
                    Collection<?> nestedOmos = (Collection<?>) o;
                    for (Object nestedOmo : nestedOmos) {
                        if ( nestedOmo == null ) {
                            continue;
                        }
                        ST nestedST = walk((OutputModelObject) nestedOmo);
                        //System.out.println("set ModelElement "+fieldName+"="+nestedST+" in "+templateName);
                        st.add(fieldName, nestedST);
                    }
                }
                /*
                 * else if ( o instanceof Map ) {
                 * Map<?, ?> nestedOmoMap = (Map<?, ?>)o;
                 * Map<Object, ST> m = new LinkedHashMap<Object, ST>();
                 * for (Map.Entry<?, ?> symbol : nestedOmoMap.entrySet()) {
                 * ST nestedST = walk((OutputModelObject)symbol.getValue());
                 * //
                 * System.out.println("set ModelElement "+fieldName+"="+nestedST
                 * +" in "+templateName);
                 * m.put(symbol.getKey(), nestedST);
                 * }
                 * st.add(fieldName, m);
                 * }
                 */
                else if ( o != null ) {
                    compiler.errorManager.toolError(ErrorKind.INTERNAL_ERROR,
                            "unrecognized nested model element: " + fieldName);
                }
            }
            catch (IllegalAccessException iae) {
                compiler.errorManager.toolError(
                        ErrorKind.CODE_TEMPLATE_ARG_ISSUE, templateName,
                        fieldName);
            }
        }
        return st;
    }
}