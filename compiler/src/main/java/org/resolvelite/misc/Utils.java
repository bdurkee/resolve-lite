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
package org.resolvelite.misc;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.resolvelite.parsing.ResolveParser;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/** A collection of general purposes classes, interfaces, and methods.
 *  @author dtwelch <dtw.welch@gmail.com>
 */
public class Utils {

    /** A builder of objects of type {@code T}.
     *  @param <T> The type object to be created.
     */
    public interface Builder<T> {
        T build();
    }

    public static ParserRuleContext getAncestor(Parser parser,
            ParserRuleContext ctx, String ruleName) {
        int ruleIndex = parser.getRuleIndex(ruleName);
        return getAncestor(ctx, ruleIndex);
    }

    /** Returns first ancestor node up the chain towards the root that has the
     *  rule index. Search includes the current node.
     */
    public static ParserRuleContext getAncestor(ParserRuleContext ctx,
            int ruleIndex) {
        while (ctx != null) {
            if ( ctx.getRuleIndex() == ruleIndex ) {
                return ctx;
            }
            ctx = ctx.getParent();
        }
        return null;
    }

    public static String getModuleName(@NotNull ParseTree ctx) {
        if ( ctx instanceof ResolveParser.ModuleContext ) {
            ctx = ctx.getChild(0);
        }

        if ( ctx instanceof ResolveParser.PrecisModuleContext ) {
            return ((ResolveParser.PrecisModuleContext) ctx).name.getText();
        }
        else if ( ctx instanceof ResolveParser.ConceptModuleContext ) {
            return ((ResolveParser.ConceptModuleContext) ctx).name.getText();
        }
        else if ( ctx instanceof ResolveParser.FacilityModuleContext ) {
            return ((ResolveParser.FacilityModuleContext) ctx).name.getText();
        }
        else if ( ctx instanceof ResolveParser.ConceptImplModuleContext ) {
            return ((ResolveParser.ConceptImplModuleContext) ctx).name
                    .getText();
        }
        else {
            throw new IllegalArgumentException("unrecognized module");
        }
    }

    public static String getNameFromProgramOp(String op) {
        String name = null;
        if ( op.equals("+") )
            name = "Sum";
        else if ( op.equals("-") )
            name = "Difference";
        else if ( op.equals("*") )
            name = "Product";
        else if ( op.equals("/") )
            name = "Divide";
        else if ( op.equals("<=") )
            name = "Less_Or_Equal";
        else if ( op.equals("<") )
            name = "Less";
        else if ( op.equals("=") )
            name = "Are_Equal";
        else if ( op.equals("/=") )
            name = "Are_Not_Equal";
        else if ( op.equals(">") )
            name = "Greater";
        else if ( op.equals(">=") )
            name = "Greater_Or_Equal";
        else
            throw new IllegalArgumentException("No template operation "
                    + "corresponding to operator: " + op);
        return name;
    }

    public static <T> String join(Collection<T> data, String separator) {
        return join(data.iterator(), separator, "", "");
    }

    public static <T> String join(Collection<T> data, String separator,
            String left, String right) {
        return join(data.iterator(), separator, left, right);
    }

    public static <T> String join(Iterator<T> iter, String separator,
            String left, String right) {
        StringBuilder buf = new StringBuilder();

        while (iter.hasNext()) {
            buf.append(iter.next());
            if ( iter.hasNext() ) {
                buf.append(separator);
            }
        }
        return left + buf.toString() + right;
    }

    public static <T> String join(T[] array, String separator) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < array.length; ++i) {
            builder.append(array[i]);
            if ( i < array.length - 1 ) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    public static String tab(int n) {
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i <= n; i++)
            buf.append("    ");
        return buf.toString();
    }

    /**
     * Strips leading directories off a file's name; for example:
     * {@code ../Foo/facilities/T.concept} grooms to {@code T.concept}.
     * 
     * @param name A file name with zero or more '/' delimited directories.
     * @return just the file name.
     */
    public static String groomFileName(String name) {
        int start = name.lastIndexOf("/");
        if ( start == -1 ) {
            return name;
        }
        return name.substring(start + 1, name.length());
    }

    public static String stripFileExtension(String name) {
        if ( name == null ) return null;
        int lastDot = name.lastIndexOf('.');
        if ( lastDot < 0 ) return name;
        return name.substring(0, lastDot);
    }
}
