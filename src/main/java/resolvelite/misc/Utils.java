/**
 * Utils.java
 * ---------------------------------
 * Copyright (c) 2014
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package resolvelite.misc;

import java.util.*;

public class Utils {

    public static class Indirect<T> {

        public T data;
    }

    /**
     * <p>
     * A builder of objects of type <code>T</code>.
     * </p>
     * 
     * @param <T> The type object to be created.
     */
    public interface Builder<T> {

        T build();
    }

    /**
     * <p>
     * A two-parameter mapping.
     * </p>
     */
    public interface Mapping<I, O> {

        public O map(I input);
    }

    /**
     * <p>
     * A three-parameter mapping.
     * </p>
     */
    public interface Mapping3<P1, P2, P3, R> {

        public R map(P1 p1, P2 p2, P3 p3);
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
            if (iter.hasNext()) {
                buf.append(separator);
            }
        }
        return left + buf.toString() + right;
    }

    public static <T> String join(T[] array, String separator) {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < array.length; ++i) {
            builder.append(array[i]);
            if (i < array.length - 1) {
                builder.append(separator);
            }
        }
        return builder.toString();
    }

    public static String groomFileName(String name) {
        int start = name.lastIndexOf("/");
        if (start == -1) {
            return name;
        }
        return name.substring(start + 1, name.length());
    }

    public static String stripFileExtension(String name) {
        if (name == null) {
            return null;
        }
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) {
            return name;
        }
        return name.substring(0, lastDot);
    }
}
