package org.rsrg.semantics;

import edu.clemson.resolve.compiler.ErrorManager;
import edu.clemson.resolve.misc.Utils;

import java.util.*;

public class MTCartesian extends MTType {

    private static final int BASE_HASH = "MTCartesian".hashCode();

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

    public MTType getFactor(String tag) {
        MTType result;
        if ( elements.get(0).tag != null && elements.get(0).tag.equals(tag) ) {
            result = elements.get(0).element;
        }
        else if ( elements.get(1).tag != null
                && elements.get(1).tag.equals(tag) ) {
            result = elements.get(1).element;
        }
        else if ( elements.get(0).element instanceof MTCartesian ) {
            result = ((MTCartesian) elements.get(0).element).getFactor(tag);
        }
        else {
            throw new NoSuchElementException();
        }
        return result;
    }

    public int size() {
        return size;
    }

    public String getTag(int index) {
        return getElement(index).tag;
    }

    private Element getElement(int index) {
        Element result;
        if ( index < 0 || index >= size ) {
            throw new IndexOutOfBoundsException("" + index);
        }
        if ( index == (size - 1) ) {
            result = elements.get(1);
        }
        else {
            if ( size == 2 ) {
                //ASSERT: myElements.get(0) cannot be an instance of MTCartesian
                if ( index != 0 ) {
                    throw new IndexOutOfBoundsException("" + index);
                }

                result = elements.get(0);
            }
            else {
                //ASSERT: myElements.get(0) MUST be an instance of MTCartesian
                result =
                        ((MTCartesian) elements.get(0).element)
                                .getElement(index);
            }
        }
        return result;
    }

    public MTType getFactor(int index) {
        return getElement(index).element;
    }

    @Override public List<MTType> getComponentTypes() {
        return elementTypes;
    }

    @Override public MTType withComponentReplaced(int index, MTType newType) {
        List<Element> newElements = new LinkedList<>(elements);
        newElements
                .set(index, new Element(newElements.get(index).tag, newType));
        return new MTCartesian(getTypeGraph(), newElements);
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

    @Override public int getHashCode() {
        int result = BASE_HASH;

        for (Element t : elements) {
            result *= 37;
            result += t.element.hashCode();
        }
        return result;
    }

    @Override public void acceptOpen(TypeVisitor v) {
        v.beginMTType(this);
        v.beginMTCartesian(this);
    }

    @Override public void accept(TypeVisitor v) {
        acceptOpen(v);
        v.beginChildren(this);
        for (Element t : this.elements) {
            t.element.accept(v);
        }
        v.endChildren(this);
        acceptClose(v);
    }

    @Override public void acceptClose(TypeVisitor v) {
        v.endMTCartesian(this);
        v.endMTType(this);
    }
}
