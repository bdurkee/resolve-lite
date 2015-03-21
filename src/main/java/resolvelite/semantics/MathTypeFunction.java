package resolvelite.semantics;

import resolvelite.typereasoning.TypeGraph;

import java.util.*;

/**
 * Created by daniel on 3/21/15.
 */
public class MathTypeFunction extends MathType {

    private final MathType domain, range;

    public MathTypeFunction(TypeGraph g, MathType range,
            List<MathType> paramTypes, String singleParameterName) {
        this(g, range, Collections.singletonList(singleParameterName),
                paramTypes);
    }

    public MathTypeFunction(TypeGraph g, MathType range,
            List<MathType> paramTypes) {
        this(g, range, buildNullNameListOfEqualLength(paramTypes), paramTypes);
    }

    public MathTypeFunction(TypeGraph g, MathType range,
            List<String> paramNames, List<MathType> paramTypes) {
        super(g);
        this.domain = buildParameterType(g, paramNames, paramTypes);
        this.range = range;

    }

    private static List<String> buildNullNameListOfEqualLength(
            List<MathType> original) {
        List<String> names = new ArrayList<>();

        for (@SuppressWarnings("unused") MathType t : original) {
            names.add(null);
        }
        return names;
    }

    public static MathType buildParameterType(TypeGraph g,
            List<String> paramNames, List<MathType> paramTypes) {
        MathType result;

        switch (paramTypes.size()) {
        case 0:
            result = g.VOID;
            break;
        case 1:
            result = paramTypes.get(0);
            break;
        default:
            List<MathTypeCartesian.Element> elements = new ArrayList<>();
            Iterator<String> namesIter = paramNames.iterator();
            Iterator<MathType> typesIter = paramTypes.iterator();
            while (namesIter.hasNext()) {
                elements.add(new MathTypeCartesian.Element(typesIter.next(),
                        namesIter.next()));
            }
            result = new MathTypeCartesian(g, elements);
        }
        return result;
    }

}
