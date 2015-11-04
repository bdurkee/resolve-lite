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

import edu.clemson.resolve.parser.ResolveLexer;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Some generally useful methods and interfaces.
 *
 * @author daniel <dtw.welch@gmail.com>
 */
public class Utils {

    @NotNull public static <T, R> List<R> apply(@NotNull Collection<T> l,
                                                @NotNull Function<T, R> f) {
        return l.stream().map(f).collect(Collectors.toList());
    }

    public static <T, R> void apply(@NotNull Collection<T> input,
                                    @NotNull Collection<R> accumulator,
                                    @NotNull Function<T, Collection<R>> f) {
        for (T t : input) {
            accumulator.addAll(f.apply(t));
        }
    }

    @NotNull public static <T> String join(@NotNull Collection<T> data,
                                           @NotNull String separator) {
        return join(data.iterator(), separator, "", "");
    }

    @NotNull public static <T> String join(@NotNull Collection<T> data,
                                           @NotNull String separator,
                                           @NotNull String left,
                                           @NotNull String right) {
        return join(data.iterator(), separator, left, right);
    }

    @NotNull public static <T> String join(@NotNull Iterator<T> iter,
                                           @NotNull String separator,
                                           @NotNull String left, String right) {
        StringBuilder buf = new StringBuilder();

        while (iter.hasNext()) {
            buf.append(iter.next());
            if ( iter.hasNext() ) {
                buf.append(separator);
            }
        }
        return left + buf.toString() + right;
    }

    public static <T> String join(@NotNull T[] array,
                                  @NotNull String separator) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < array.length; ++i) {
            builder.append(array[i]);
            if ( i < array.length - 1 ) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    public static <T, R> Map<T, R> zip(@NotNull List<T> l1,
                                       @NotNull List<R> l2)
            throws IllegalArgumentException {
        if (l1.size() != l2.size()) {
            throw new IllegalArgumentException("attempt to zip differently " +
                    "sized lists");
        }
        Map<T, R> result = new LinkedHashMap<>();
        Iterator<R> l2iter = l2.iterator();
        for (T t : l1) {
            result.put(t, l2iter.next());
        }
        return result;
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
            @NotNull Class<T> expectedType,
            @NotNull List<? extends ParseTree> nodes,
            @NotNull ParseTreeProperty<? extends E> annotations) {
        return nodes.stream().map(x -> expectedType
                .cast(annotations.get(x))).collect(Collectors.toList());
    }

    public static String getModuleName(@NotNull ParseTree ctx) {
        if (ctx instanceof ResolveParser.ModuleContext) {
            ctx = ctx.getChild(0);
        }

        if (ctx instanceof ResolveParser.PrecisModuleContext ) {
            return ((ResolveParser.PrecisModuleContext) ctx).name.getText();
        }
        /*else if ( ctx instanceof Resolve.ConceptModuleContext ) {
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
        else if ( ctx instanceof Resolve.PrecisExtensionModuleContext ) {
            return ((Resolve.PrecisExtensionModuleContext) ctx).name
                    .getText();
        }*/
        else {
            throw new IllegalArgumentException("unrecognized module");
        }
    }

    public interface Builder<T> {
        @NotNull T build();
    }

    /**
     * Returns a new {@link CommonToken} given some arbitrary, parser created
     * {@link Token} {@code t}. This is useful for when you want create a token
     * consisting of {@code desiredText} but with location information
     * 'filled-in' and accounted for -- taken from {@code t}.
     * <p>
     * <strong>NOTE:</strong> if {@code desiredText} is {@code null}, then
     * the text for the resulting {@code CommonToken} will contain whatever
     * text existed in {@code t} starting out.</p>
     *
     * @param t An existing token (preferablly near where {@code desiredText} should appear)
     * @param desiredText The text we want the resulting token to hold
     * @return a new token
     */
    public static CommonToken createTokenFrom(@NotNull Token t,
                                              @NotNull String desiredText) {
        CommonToken result = new CommonToken(t);
        result.setText(desiredText);
        return result;
    }

    /**
     * Returns the text encapsulated by a {@link ParserRuleContext} exactly
     * as it appears within whatever sourcecode the user typed in.
     *
     * @param ctx the rule context
     * @return the raw sourcecode represented within {@code ctx}
     */
    public static String getRawText(@Nullable ParserRuleContext ctx) {
        if (ctx == null) return "";
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
