package edu.clemson.resolve.semantics;

import edu.clemson.resolve.misc.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import edu.clemson.resolve.semantics.symbol.MathClssftnWrappingSymbol;

import java.util.*;

public class MathCartesianClssftn extends MathClssftn {

    private final List<Element> elements = new ArrayList<>();
    public final Map<String, MathClssftnWrappingSymbol> syms = new LinkedHashMap<>();
    public final Map<String, Element> tagsToElements = new LinkedHashMap<>();

    public MathCartesianClssftn(DumbMathClssftnHandler g, Element... e) {
        this(g, Arrays.asList(e));
    }

    public MathCartesianClssftn(DumbMathClssftnHandler g, List<Element> elements) {
        super(g, g.CLS);
        this.elements.addAll(elements);
        this.typeRefDepth = 1;
        for (Element e : elements) {
            tagsToElements.put(e.getTag(), e);
        }
    }

    @Override
    public List<MathClssftn> getComponentTypes() {
        List<MathClssftn> result = new ArrayList<>();
        for (Element e : elements) {
            result.add(e.clssfcn);
        }
        return result;
    }

    @Override
    public int getTypeRefDepth() {
        return 1;
    }

    @Override
    public MathClssftn withVariablesSubstituted(
            Map<String, MathClssftn> substitutions) {
        List<Element> newElements = new ArrayList<>();
        for (Element element : elements) {
            newElements.add(
                    new Element(element.getTag(), element.clssfcn
                            .withVariablesSubstituted(substitutions)));
        }
        return new MathCartesianClssftn(g, newElements);
    }

    public int size() {
        return elements.size();
    }

    public MathClssftn getFactor(int i) {
        return elements.get(i).clssfcn;
    }

    @Nullable
    public MathClssftn getFactor(String tag) {
        if (tagsToElements.get(tag) == null)
            throw new NoSuchElementException(tag);
        return tagsToElements.get(tag).clssfcn;
    }

    @Nullable
    public Element getElementUnder(String tag) {
        return tagsToElements.get(tag);
    }

    @Override
    public String toString() {
        return "(" + Utils.join(elements, " * ") + ")";
    }

    public static class Element {
        private final String tag;
        public MathClssftn clssfcn;

        public Element(@Nullable String tag, @NotNull MathClssftn c) {
            this.tag = tag;
            this.clssfcn = c;
        }

        public Element(@NotNull MathClssftn c) {
            this(null, c);
        }

        @Nullable
        public String getTag() {
            return tag;
        }

        @Override
        public String toString() {
            String result = clssfcn.toString();
            if (tag != null && !tag.equals("")) {
                String colonOp = " : ";
                if (clssfcn == clssfcn.getTypeGraph().CLS) colonOp = " ː ";
                result = "(" + tag + colonOp + clssfcn + ")";
            }
            return result;
        }
    }
}
