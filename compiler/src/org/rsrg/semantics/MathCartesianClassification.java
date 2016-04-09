package org.rsrg.semantics;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class MathCartesianClassification extends MathClassification {

    private final List<Element> elements = new ArrayList<>();
    private final List<MathClassification> elementClssfctns =
            new ArrayList<>();
    private final Map<String, Element> tagsToElements =
            new LinkedHashMap<>();

    public MathCartesianClassification(DumbTypeGraph g, Element ... e) {
        this(g, Arrays.asList(e));
    }

    public MathCartesianClassification(DumbTypeGraph g,
                                       List<Element> elements) {
        super(g, g.CLS);
        this.elements.addAll(elements);
        this.elementClssfctns.addAll(
                Utils.apply(elements, Element::getClassification));
        for (Element e : elements) {
            tagsToElements.put(e.getTag(), e);
        }
    }

    @Override public List<MathClassification> getComponentTypes() {
        return elementClssfctns;
    }

    @Override public int getTypeRefDepth() {
        return 1;
    }

    @Override public MathClassification withVariablesSubstituted(
            Map<MathClassification, MathClassification> substitutions) {
        List<Element> newElements = new ArrayList<>();
        for (Element element : elements) {
            newElements.add(
                    new Element(element.getTag(), element.getClassification()
                            .withVariablesSubstituted(substitutions)));
        }
        return new MathCartesianClassification(g, newElements);
    }

    public int size() {
        return elements.size();
    }

    public MathClassification getFactor(int i) {
        return elements.get(i).getClassification();
    }

    @Nullable public MathClassification getFactor(String tag) {
        return tagsToElements.get(tag).clssfcn;
    }

    @Override public String toString() {
        return "(" + Utils.join(elements, " * ") + ")";
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
            if (tag != null && !tag.equals("")) {
                String colonOp = " : ";
                if (clssfcn == clssfcn.getTypeGraph().CLS) colonOp = " ː ";
                result = "("+tag+colonOp+clssfcn+")";
            }
            return result;
        }
    }
}
