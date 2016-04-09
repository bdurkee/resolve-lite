package org.rsrg.semantics;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MathCartesianClassification extends MathClassification {

    private final List<MathClassification> components = new LinkedList<>();
    private final List<Element> elements = new ArrayList<>();

    public MathCartesianClassification(DumbTypeGraph g,
                                       List<MathClassification> componentTypes) {
        super(g, g.INVALID);
        components.addAll(componentTypes);
        //I'm type permissible if each component is type permissible.. right?
    }

    @Override public List<MathClassification> getComponentTypes() {
        return components;
    }

    @Override public int getTypeRefDepth() {
        return 1;
    }

    @Override public MathClassification withVariablesSubstituted(
            Map<MathClassification, MathClassification> substitutions) {
        List<MathClassification> newComponents = new ArrayList<>();
        for (MathClassification component : components) {
            newComponents.add(
                    component.withVariablesSubstituted(substitutions));
        }
        return new MathCartesianClassification(g, newComponents);
    }

    public int size() {
        return components.size();
    }

    public MathClassification getFactor(int i) {
        return components.get(i);
    }

    @Override public String toString() {
        return "(" + Utils.join(components, " * ") + ")";
    }

    public static class Element {
        private final String tag;
        private final MathClassification clssfcn;

        public Element(@Nullable String tag, @NotNull MathClassification c) {
            this.tag = tag;
            this.clssfcn = c;
        }

        public Element(@NotNull MathClassification c) {
            this(null, c);
        }

        @Nullable public String getTag() {
            return tag;
        }

        @NotNull public MathClassification getClassification() {
            return clssfcn;
        }

        @Override public String toString() {
            String result = clssfcn.toString();
            if (tag != null) {
                String colonOp = " : ";
                if (clssfcn == clssfcn.getTypeGraph().CLS) colonOp = " ː ";
                result = "("+tag+colonOp+clssfcn+")";
            }
            return result;
        }
    }
}
