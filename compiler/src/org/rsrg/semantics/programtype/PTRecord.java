package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.DumbTypeGraph;
import org.rsrg.semantics.MathClassification;
import org.rsrg.semantics.symbol.FacilitySymbol;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class PTRecord extends ProgType {

    @NotNull private final Map<String, ProgType> fields = new HashMap<>();
    @NotNull private MathClassification mathTypeAlterEgo;

    public PTRecord(@NotNull DumbTypeGraph g,
                    @NotNull Map<String, ProgType> types) {
        super(g);
        this.fields.putAll(types);

       /* Element[] elements = new Element[types.size()];
        int index = 0;
        for (Map.Entry<String, ProgType> field : types.entrySet()) {
            elements[index] =
                    new Element(field.getKey(), field.getValue().toMath());
            index++;
        }
        this.mathTypeAlterEgo = new MathCartesianClassification(g, elements);*/
    }

    @NotNull public ProgType getFieldType(@NotNull String name)
            throws NoSuchElementException {
        ProgType result = fields.get(name);
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }

    @Override public boolean isAggregateType() {
        return true;
    }

    @NotNull @Override public MathClassification toMath() {
        return mathTypeAlterEgo;
    }

    @Override public String toString() {
        return "record " + fields;
    }

    @NotNull @Override public ProgType instantiateGenerics(
            @NotNull Map<String, ProgType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {

        Map<String, ProgType> newFields = new HashMap<>();
        for (Map.Entry<String, ProgType> type : fields.entrySet()) {
            newFields.put(
                    type.getKey(),
                    type.getValue().instantiateGenerics(genericInstantiations,
                            instantiatingFacility));
        }
        return new PTRecord(getTypeGraph(), newFields);
    }
}
