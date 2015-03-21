package resolvelite.semantics;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.typereasoning.TypeGraph;

import java.util.ArrayList;
import java.util.List;

public class MathTypeCart extends MathType {

    private final List<Element> elements = new ArrayList<>();

    public MathTypeCart(@NotNull TypeGraph g, List<Element> elements) {
        super(g);
        this.elements.addAll(elements);
    }

    public static class Element {

        MathType elementType;
        String elementName;

        public Element(MathType type, String name) {
            this.elementName = name;
            this.elementType = type;
        }

        @Override
        public String toString() {
            return "(" + elementName + ":" + elementType + ")";
        }
    }
}
