package org.rsrg.semantics.programtype;

import org.jetbrains.annotations.NotNull;
import org.rsrg.semantics.MTCartesian;
import org.rsrg.semantics.MTCartesian.Element;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.symbol.FacilitySymbol;
import org.rsrg.semantics.TypeGraph;

import java.util.HashMap;
import java.util.Map;

public class PTRecord extends PTType {

    @NotNull private final Map<String, PTType> fields = new HashMap<>();
    @NotNull private MTType mathTypeAlterEgo;

    public PTRecord(@NotNull TypeGraph g,
                    @NotNull Map<String, PTType> types) {
        super(g);
        this.fields.putAll(types);

        Element[] elements = new Element[types.size()];
        int index = 0;
        for (Map.Entry<String, PTType> field : types.entrySet()) {
            elements[index] =
                    new Element(field.getKey(), field.getValue().toMath());
            index++;
        }
        this.mathTypeAlterEgo = new MTCartesian(g, elements);
    }

    @NotNull public PTType getFieldType(@NotNull String name) {
        return fields.get(name);
    }

    @Override public boolean isAggregateType() {
        return true;
    }

    @NotNull @Override public MTType toMath() {
        return mathTypeAlterEgo;
    }

    @Override public String toString() {
        return "record " + fields;
    }

    @NotNull @Override public PTType instantiateGenerics(
            @NotNull Map<String, PTType> genericInstantiations,
            @NotNull FacilitySymbol instantiatingFacility) {

        Map<String, PTType> newFields = new HashMap<>();
        for (Map.Entry<String, PTType> type : fields.entrySet()) {
            newFields.put(
                    type.getKey(),
                    type.getValue().instantiateGenerics(genericInstantiations,
                            instantiatingFacility));
        }
        return new PTRecord(getTypeGraph(), newFields);
    }
}
