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
package resolvelite.misc;

import org.antlr.v4.runtime.tree.ParseTree;
import resolvelite.parsing.ResolveParser;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A collection of general-purpose methods.
 * 
 * @author dwelch <dtw.welch@gmail.com>
 */
public class Utils {

    public static class Indirect<T> {

        public T data;
    }

    /**
     * A builder of objects of type <code>T</code>.
     * 
     * @param <T> The type object to be created.
     */
    public interface Builder<T> {

        T build();
    }

    /**
     * A two-parameter mapping.
     */
    public interface Mapping<I, O> {

        public O map(I input);
    }

    /**
     * A three-parameter mapping.
     */
    public interface Mapping3<P1, P2, P3, R> {

        public R map(P1 p1, P2 p2, P3 p3);
    }

    public static <T> List<T> filter(List<T> data, Predicate<T> pred) {
        List<T> output = new ArrayList<T>();
        for (T x : data) {
            if ( pred.test(x) ) {
                output.add(x);
            }
        }
        return output;
    }

    public static <T, R> List<R> map(List<T> data, Function<T, R> getter) {
        List<R> output = new ArrayList<R>();
        for (T x : data) {
            output.add(getter.apply(x));
        }
        return output;
    }

    public static <T, R> List<R> map(T[] data, Function<T, R> getter) {
        List<R> output = new ArrayList<R>();
        for (T x : data) {
            output.add(getter.apply(x));
        }
        return output;
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

    public static String extractModuleName(ParseTree ctx) {
        String result = null;
        //In case the user passes a plain ModuleContext node.
        if ( ctx instanceof ResolveParser.ModuleContext ) {
            ctx = ctx.getChild(0); //specific module-ctxs are zeroth child of
            // of the ModuleContext rule/context.
        }
        if ( ctx instanceof ResolveParser.PrecisModuleContext ) {
            ResolveParser.PrecisModuleContext ctxAsPrecisModule =
                    (ResolveParser.PrecisModuleContext) ctx;
            result = ctxAsPrecisModule.name.getText();
        }
        else if ( ctx instanceof ResolveParser.ConceptModuleContext ) {
            ResolveParser.PrecisModuleContext ctxAsPrecisModule =
                    (ResolveParser.PrecisModuleContext) ctx;
            result = ctxAsPrecisModule.name.getText();
        }
        else {
            throw new IllegalArgumentException("cannot retrieve module name "
                    + "from rule context: " + ctx.getClass());
        }
        return result;
    }

    public static String tab(int n) {
        StringBuilder buf = new StringBuilder();
        for (int i = 1; i <= n; i++)
            buf.append("    ");
        return buf.toString();
    }

    public static String groomFileName(String name) {
        int start = name.lastIndexOf("/");
        if ( start == -1 ) {
            return name;
        }
        return name.substring(start + 1, name.length());
    }

    public static String stripFileExtension(String name) {
        if ( name == null ) {
            return null;
        }
        int lastDot = name.lastIndexOf('.');
        if ( lastDot < 0 ) {
            return name;
        }
        return name.substring(0, lastDot);
    }
}
