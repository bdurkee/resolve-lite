package org.resolvelite.semantics;

import org.resolvelite.typereasoning.TypeGraph;

import java.util.*;

public class MTCartesian extends MTType {

    private List<Element> elements = new ArrayList<>();
    private List<MTType> elementTypes = new ArrayList<>();
    private Map<String, Element> tagsToElementsTable = new HashMap<>();
    private Map<Element, String> elementsToTagsTable = new HashMap<>();

    private final int mySize;

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
            throw new IllegalArgumentException(
                    "Unexpected cartesian product size.");
        }
        int workingSize = 0;

        Element first;
        if ( elementCount == 2 ) {
            first = new Element(elements[0]);
        }
        else {
            first = new Element(new MTCartesian(g, elements, elementCount - 1));
        }

        if ( first.myElement instanceof MTCartesian ) {
            workingSize += ((MTCartesian) first.myElement).size();
        }
        else {
            workingSize += 1;
        }

        Element second = new Element(elements[elementCount - 1]);
        workingSize += 1;

        first.addTo(this.elements, elementTypes, tagsToElementsTable,
                elementsToTagsTable);
        second.addTo(this.elements, elementTypes, tagsToElementsTable,
                elementsToTagsTable);
        mySize = workingSize;
        elementTypes = Collections.unmodifiableList(elementTypes);
    }

    public int size() {
        return mySize;
    }

    private Element getElement(int index) {
        Element result;

        if ( index < 0 || index >= mySize ) {
            throw new IndexOutOfBoundsException("" + index);
        }

        if ( index == (mySize - 1) ) {
            result = elements.get(1);
        }
        else {
            if ( mySize == 2 ) {
                //ASSERT: elements.get(0) cannot be an instance of MTCartesian
                if ( index != 0 ) {
                    throw new IndexOutOfBoundsException("" + index);
                }

                result = elements.get(0);
            }
            else {
                //ASSERT: elements.get(0) MUST be an instance of MTCartesian
                result =
                        ((MTCartesian) elements.get(0).myElement)
                                .getElement(index);
            }
        }

        return result;
    }

    public String getTag(int index) {
        return getElement(index).myTag;
    }

    public MTType getFactor(int index) {
        return getElement(index).myElement;
    }

    public MTType getFactor(String tag) {
        MTType result;

        if ( elements.get(0).myTag != null && elements.get(0).myTag.equals(tag) ) {
            result = elements.get(0).myElement;
        }
        else if ( elements.get(1).myTag != null
                && elements.get(1).myTag.equals(tag) ) {
            result = elements.get(1).myElement;
        }
        else if ( elements.get(0).myElement instanceof MTCartesian ) {
            result = ((MTCartesian) elements.get(0).myElement).getFactor(tag);
        }
        else {
            throw new NoSuchElementException();
        }

        return result;
    }

    @Override public String toString() {
        StringBuffer str = new StringBuffer("(");
        Iterator<Element> types = elements.iterator();
        while (types.hasNext()) {
            str.append(types.next().toString());
            if ( types.hasNext() ) {
                str.append(" * ");
            }
        }
        str.append(")");
        return str.toString();
    }

    @Override public List<MTType> getComponentTypes() {
        return elementTypes;
    }

    public static class Element {

        private String myTag;
        private MTType myElement;

        public Element(Element element) {
            this(element.myTag, element.myElement);
        }

        public Element(MTType element) {
            this(null, element);
        }

        public Element(String tag, MTType element) {
            if ( element == null ) {
                throw new IllegalArgumentException("Element \"" + tag + "\" "
                        + "has null type.");
            }

            myElement = element;
            myTag = tag;
        }

        @Override public String toString() {
            String result = myElement.toString();

            if ( myTag != null ) {
                result = "(" + myTag + " : " + result + ")";
            }

            return result;
        }

        private void addTo(List<Element> elements, List<MTType> elementTypes,
                Map<String, Element> tagsToElements,
                Map<Element, String> elementsToTags) {

            elements.add(this);
            elementTypes.add(myElement);

            if ( myTag != null ) {

                if ( tagsToElements.containsKey(myTag) ) {
                    throw new IllegalArgumentException("Duplicate tag: "
                            + myTag);
                }

                tagsToElements.put(myTag, this);
                elementsToTags.put(this, myTag);
            }
        }
    }
}
