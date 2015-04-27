package org.resolvelite.semantics;

import org.resolvelite.compiler.ErrorManager;
import org.resolvelite.misc.Utils;
import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;

public class MTCartesian extends MTType {

    private List<Element> elements = new LinkedList<>();
    private List<MTType> elementTypes = new LinkedList<>();
    private final int size;

    public MTCartesian(TypeGraph g, Element... elements) {
        this(g, elements, elements.length);
    }

    public MTCartesian(TypeGraph g, List<Element> elements) {
        this(g, elements.toArray(new Element[0]), elements.size());
    }

    private MTCartesian(TypeGraph g, Element[] elements, int elementCount) {
        super(g);

        if ( elementCount < 2 ) {
            //We assert this isn't possible, but who knows?
            ErrorManager.fatalInternalError(
                    "unexpected cartesian product size",
                    new IllegalArgumentException());
        }
        int workingSize = 0;
        Element first;
        if ( elementCount == 2 ) {
            first = new Element(elements[0]);
        }
        else {
            first = new Element(new MTCartesian(g, elements, elementCount - 1));
        }

        if ( first.element instanceof MTCartesian ) {
            workingSize += ((MTCartesian) first.element).size();
        }
        else {
            workingSize += 1;
        }

        Element second = new Element(elements[elementCount - 1]);
        workingSize += 1;

        Map<String, Element> tagsToElementsTable = new LinkedHashMap<>();
        Map<Element, String> elementsToTagsTable = new LinkedHashMap<>();
        first.addTo(this.elements, elementTypes, tagsToElementsTable,
                elementsToTagsTable);
        second.addTo(this.elements, elementTypes, tagsToElementsTable,
                elementsToTagsTable);
        this.size = workingSize;
        this.elementTypes = new ArrayList<>(elementTypes);
    }

    public int size() {
        return size;
    }

    @Override public List<? extends MTType> getComponentTypes() {
        return elementTypes;
    }

    @Override public String toString() {
        return "(" + Utils.join(elements, " * ") + ")";
    }

    public static class Element {
        private String tag;
        private MTType element;

        public Element(Element element) {
            this(element.tag, element.element);
        }

        public Element(MTType element) {
            this(null, element);
        }

        public Element(String tag, MTType element) {
            if ( element == null ) {
                throw new IllegalArgumentException("element \"" + tag + "\" "
                        + "has null type");
            }
            this.element = element;
            this.tag = tag;
        }

        @Override public String toString() {
            String result = element.toString();
            if ( tag != null ) {
                result = "(" + tag + " : " + result + ")";
            }
            return result;
        }

        private void addTo(List<Element> elements, List<MTType> elementTypes,
                Map<String, Element> tagsToElements,
                Map<Element, String> elementsToTags) {
            elements.add(this);
            elementTypes.add(element);
            if ( tag != null ) {
                if ( tagsToElements.containsKey(tag) ) {
                    throw new IllegalArgumentException("duplicate tag: " + tag);
                }
                tagsToElements.put(tag, this);
                elementsToTags.put(this, tag);
            }
        }
    }
}
