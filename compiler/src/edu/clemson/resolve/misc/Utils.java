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
package edu.clemson.resolve.misc;

import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveLexer;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Some generally useful methods and interfaces.
 *
 * @author daniel <dtw.welch@gmail.com>
 */
public class Utils {

    public static <T, R> List<R> apply(Collection<T> l, Function<T, R> f) {
        return l.stream().map(f).collect(Collectors.toList());
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

    /**
     * Returns a list of {@code E} given: an expected type {@code T}, some
     * number
     * of concrete syntax {@code nodes}, and a mapping from rule contexts to
     * some number of elements descending from {@code E}.
     *
     * @param expectedType The class type to inhabit the returned list
     * @param nodes A list of concrete syntax nodes, as obtained through
     *        a visitor, listener, etc.
     * @param annotations A map from rule context to the primary supertype
     *        of {@code expectedType} ({@code E}).
     * @param <E> Super type of {@code expectedType}.
     * @param <T> The expected type.
     * @return A list of {@code T}.
     */
    public static <E, T extends E> List<T> collect(
            Class<T> expectedType, List<? extends ParseTree> nodes,
            ParseTreeProperty<? extends E> annotations) {
        return nodes.stream().map(x -> expectedType
                .cast(annotations.get(x))).collect(Collectors.toList());
    }

    public static String getModuleName(ParseTree ctx) {
        if ( ctx instanceof Resolve.ModuleContext ) {
            ctx = ctx.getChild(0);
        }

        if ( ctx instanceof Resolve.PrecisModuleContext ) {
            return ((Resolve.PrecisModuleContext) ctx).name.getText();
        }
        else if ( ctx instanceof Resolve.ConceptModuleContext ) {
            return ((Resolve.ConceptModuleContext) ctx).name.getText();
        }
        else if ( ctx instanceof Resolve.FacilityModuleContext ) {
            return ((Resolve.FacilityModuleContext) ctx).name.getText();
        }
        else if ( ctx instanceof Resolve.ConceptImplModuleContext ) {
            return ((Resolve.ConceptImplModuleContext) ctx).name
                    .getText();
        }
        else if ( ctx instanceof Resolve.EnhancementModuleContext ) {
            return ((Resolve.EnhancementModuleContext) ctx).name
                    .getText();
        }
        else if ( ctx instanceof Resolve.EnhancementImplModuleContext ) {
            return ((Resolve.EnhancementImplModuleContext) ctx).name
                    .getText();
        }
        else {
            throw new IllegalArgumentException("unrecognized module");
        }
    }

    public static BuiltInOpAttributes convertProgramOp(Token op) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);
        switch (op.getType()) {
            case ResolveLexer.PLUS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Sum");
                break;
            case ResolveLexer.MINUS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Difference");
                break;
            case ResolveLexer.MULT:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Product");
                break;
            case ResolveLexer.DIVIDE:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Divide");
                break;
            case ResolveLexer.LTE:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Less_Or_Equal");
                break;
            case ResolveLexer.LT:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Less");
                break;
            case ResolveLexer.GTE:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Greater_Or_Equal");
                break;
            case ResolveLexer.GT:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Greater");
                break;
            case ResolveLexer.EQUALS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Are_Equal");
                break;
            case ResolveLexer.NEQUALS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Are_Not_Equal");
                break;
            case ResolveLexer.MINUSMINUS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Decrement");
                break;
            case ResolveLexer.PLUSPLUS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Increment");
                break;
            case ResolveLexer.PLUSPLUSPLUS:
                result = new BuiltInOpAttributes("Std_Char_Str_Fac", op, "Merger");
                break;
        }
        return result;
    }

    public static class BuiltInOpAttributes {
        public CommonToken qualifier, name;

        public BuiltInOpAttributes(Token op) {
            this.name = new CommonToken(op);
        }

        public BuiltInOpAttributes(String qualifier, Token original,
                                   String opAsText) {
            this.name = new CommonToken(original);
            this.name.setText(opAsText);
            this.qualifier = new CommonToken(original);
            this.qualifier.setText(qualifier);
        }
    }

    public interface Builder<T> {
        T build();
    }

    /**
     * Returns the text encapsulated by a {@link ParserRuleContext} exactly
     * as it appears within whatever sourcecode the user typed in.
     *
     * @param ctx the rule context
     * @return the raw sourcecode represented within {@code ctx}
     */
    public static String getRawText(ParserRuleContext ctx) {
        Interval interval =
                new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex());
        return ctx.start.getInputStream().getText(interval);
    }

    /**
     * Strips leading directories off a file's name; for example:
     *      {@code ../Foo/facilities/Basic_Natural_Number_Theory.resolve}
     * grooms to
     *      {@code Basic_Natural_Number_Theory.resolve}.
     *
     * @param name a file name with zero or more '/' delimited directories
     * @return just the file name
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

    //TODO: Add charset parameter 'StandardCharset.' etc.
    public static String readFile(String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while((line = reader.readLine()) != null ) {
            stringBuilder.append( line );
            stringBuilder.append( ls );
        }
        return stringBuilder.toString();
    }

    public static void writeFile(String dir, String fileName, String content) {
        try {
            org.antlr.v4.runtime.misc.Utils.writeFile(dir + File.separator +
                    fileName, content, "UTF-8");
        }
        catch (IOException ioe) {
            System.err.println("can't write file");
            ioe.printStackTrace(System.err);
        }
    }
}
