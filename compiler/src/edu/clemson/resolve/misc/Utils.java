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

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.parser.ResolveParser;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.Trees;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Some generally useful methods and interfaces. */
public class Utils {

    /**
     * Applies the provided function, {@code f} to all elements of {@code l}, returning a new list of elements of type
     * corresponding to the range of {@code f}.
     *
     * @param l   a starting {@link Collection} of elements
     * @param f   a function to be applied to the elements of {@code l}
     * @param <T> type of the starting collection
     * @param <R> type of resulting list
     *
     * @return a new list of type {@code R}
     */
    @NotNull
    public static <T, R> List<R> apply(@NotNull Collection<T> l, @NotNull Function<T, R> f) {
        return l.stream().map(f).collect(Collectors.toList());
    }

    public static <T, R> void apply(@NotNull Collection<T> input,
                                    @NotNull Collection<R> accumulator,
                                    @NotNull Function<T, Collection<R>> f) {
        for (T t : input) {
            accumulator.addAll(f.apply(t));
        }
    }

    @NotNull
    public static <T> String join(@NotNull Collection<T> data, @NotNull String separator) {
        return join(data.iterator(), separator, "", "");
    }

    @NotNull
    public static <T> String join(@NotNull Collection<T> data,
                                  @NotNull String separator,
                                  @NotNull String left,
                                  @NotNull String right) {
        return join(data.iterator(), separator, left, right);
    }

    @NotNull
    public static <T> String join(@NotNull Iterator<T> iter,
                                  @NotNull String separator,
                                  @NotNull String left,
                                  @NotNull String right) {
        StringBuilder buf = new StringBuilder();

        while (iter.hasNext()) {
            buf.append(iter.next());
            if (iter.hasNext()) {
                buf.append(separator);
            }
        }
        return left + buf.toString() + right;
    }

    @NotNull
    public static <T> String join(@NotNull T[] array, @NotNull String separator) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < array.length; ++i) {
            builder.append(array[i]);
            if (i < array.length - 1) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    @NotNull
    public static <T, R> Map<T, R> zip(@NotNull List<T> l1, @NotNull List<R> l2)
            throws IllegalArgumentException {
        if (l1.size() != l2.size()) {
            throw new IllegalArgumentException("attempt to zip differently sized lists");
        }
        Map<T, R> result = new LinkedHashMap<>();
        Iterator<R> l2iter = l2.iterator();
        for (T t : l1) {
            result.put(t, l2iter.next());
        }
        return result;
    }

    /**
     * Returns a list of {@code E} given: an expected type {@code T}, some number of concrete syntax {@code nodes},
     * and a mapping from rule contexts to some number of elements descending from {@code E}.
     *
     * @param expectedType the class type to inhabit the returned list
     * @param nodes        a list of concrete syntax nodes, as obtained through a visitor, listener, etc.
     * @param annotations  a map from rule context to the primary supertype of {@code expectedType} ({@code E}).
     * @param <E>          super type of {@code expectedType}.
     * @param <T>          the expected type.
     *
     * @return a list of {@code T}.
     */
    @NotNull
    public static <E, T extends E> List<T> collect(@NotNull Class<T> expectedType,
                                                   @NotNull List<? extends ParseTree> nodes,
                                                   @NotNull ParseTreeProperty<? extends E> annotations) {
        return nodes.stream().map(x -> expectedType
                .cast(annotations.get(x))).collect(Collectors.toList());
    }

    @NotNull
    public static Token getModuleName(@NotNull ParseTree ctx) {
        if (ctx instanceof ResolveParser.ModuleDeclContext) ctx = ctx.getChild(0);

        if (ctx instanceof ResolveParser.PrecisModuleDeclContext) {
            return ((ResolveParser.PrecisModuleDeclContext) ctx).name;
        }
        else if (ctx instanceof ResolveParser.PrecisExtModuleDeclContext) {
            return ((ResolveParser.PrecisExtModuleDeclContext) ctx).name;
        }
        else if (ctx instanceof ResolveParser.FacilityModuleDeclContext) {
            return ((ResolveParser.FacilityModuleDeclContext) ctx).name;
        }
        else if (ctx instanceof ResolveParser.ConceptModuleDeclContext) {
            return ((ResolveParser.ConceptModuleDeclContext) ctx).name;
        }
        else if (ctx instanceof ResolveParser.ConceptImplModuleDeclContext) {
            return ((ResolveParser.ConceptImplModuleDeclContext) ctx).name;
        }
        else if (ctx instanceof ResolveParser.ConceptExtModuleDeclContext) {
            return ((ResolveParser.ConceptExtModuleDeclContext) ctx).name;
        }
        else if (ctx instanceof ResolveParser.ConceptExtImplModuleDeclContext) {
            return ((ResolveParser.ConceptExtImplModuleDeclContext) ctx).name;
        }
        else {
            throw new IllegalArgumentException("unrecognized module");
        }
    }

    /**
     * A general purpose builder for objects of type {@code T}. This interface should be implemented by classes that
     * might benefit from incremental construction -- meaning through chained calls to a series of builder methods that
     * return back a {@code Builder} subclass.
     *
     * @param <T> the type of the object to be built
     *
     * @see edu.clemson.resolve.proving.absyn.PApply.PApplyBuilder for an example usage
     */
    @FunctionalInterface
    public interface Builder<T> {
        @NotNull
        T build();
    }

    /**
     * Returns a new {@link CommonToken} from some arbtrary existing {@code Token}. This is useful for when you want
     * create a {@code Token} consisting of {@code desiredText}, but using existing location information from {@code t}.
     * <p>
     * <strong>NOTE:</strong> if {@code desiredText} is {@code null}, then the text for the resulting {@code Token}
     * will contain whatever text was already in {@code t} starting out.</p>
     *
     * @param t           an existing token (preferably near where {@code desiredText} should appear)
     * @param desiredText the text we want the resulting token to hold
     *
     * @return a new token
     */
    @NotNull
    public static CommonToken createTokenFrom(@NotNull Token t, @Nullable String desiredText) {
        CommonToken result = new CommonToken(t);
        if (desiredText != null) {
            result.setText(desiredText);
        }
        return result;
    }

    /**
     * Returns the raw text encapsulated by a {@link ParserRuleContext} exactly as it appears within whatever
     * sourcecode the user typed in.
     *
     * @param ctx the rule context
     *
     * @return the raw sourcecode represented by {@code ctx}
     * @deprecated use {@link Trees#getNodeText)} instead
     */
    @Deprecated
    @NotNull
    public static String getRawText(@Nullable ParserRuleContext ctx) {
        if (ctx == null) return "";
        Interval interval = new Interval(ctx.start.getStartIndex(), ctx.stop.getStopIndex());
        return ctx.start.getInputStream().getText(interval);
    }

    @Nullable
    public static ParserRuleContext getFirstAncestorOfType(@Nullable ParserRuleContext t,
                                                           @NotNull Class<?>... clazzes) {
        return getFirstAncestorOfType(t, Arrays.asList(clazzes));
    }

    /**
     * Return first ancestor node up the chain towards the root that is in {@code clazzes}. Search includes the
     * current node.
     *
     * @return the found parent, {@code null} if not found.
     */
    @Nullable
    public static ParserRuleContext getFirstAncestorOfType(
            @Nullable ParserRuleContext t, @NotNull List<Class<?>> clazzes) {
        while (t != null) {
            for (Class<?> clazz : clazzes) {
                if (t.getClass() == clazz) {
                    return t;
                }
            }
            t = t.getParent();
        }
        return null;
    }

    /**
     * Given an extensionless {@code name} and a compiler instance; searches for and returns an external file of
     * name {@code name}.
     */
    @Nullable
    public static File getExternalFile(@NotNull RESOLVECompiler e, @Nullable String name) {
        if (name == null) return null;
        FileLocator l = new FileLocator(name, RESOLVECompiler.NON_NATIVE_EXTENSION);
        File result = null;
        try {
            //an external file is likely going to appear in the core lib
            //so we search there first..
            Files.walkFileTree(new File(RESOLVECompiler.getCoreLibraryDirectory()).toPath(), l);
            result = l.getFile();
        } catch (NoSuchFileException nsfe) {
            //ok, maybe they defined an external file in their own workspace?
            try {
                Files.walkFileTree(new File(e.workingDirectory).toPath(), l);
                result = l.getFile();
            } catch (IOException ignored) {
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    /**
     * Strips leading directories off a file's name; for example:
     * {@code ../Foo/precis/Nat_Num_Theory.resolve} grooms to {@code Nat_Num_Theory.resolve}.
     *
     * @param name a file name with zero or more '/' delimited directories
     *
     * @return the extensionless filename
     */
    @NotNull
    public static String groomFileName(@NotNull String name) {
        int start = name.lastIndexOf("/");
        if (start == -1) {
            return name;
        }
        return name.substring(start + 1, name.length());
    }

    @Nullable
    public static String stripFileExtension(@Nullable String name) {
        if (name == null) return null;
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) return name;
        return name.substring(0, lastDot);
    }

    //TODO: Add charset parameter 'StandardCharset.' etc.
    @Nullable
    public static String readFile(@Nullable String file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        return stringBuilder.toString();
    }

    public static void writeFile(@Nullable String dir, @Nullable String fileName, @Nullable String content) {
        try {
            org.antlr.v4.runtime.misc.Utils.writeFile(dir + File.separator + fileName, content, "UTF-8");
        } catch (IOException ioe) {
            System.err.println("can't write file");
            ioe.printStackTrace(System.err);
        }
    }
}
