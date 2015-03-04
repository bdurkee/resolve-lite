package resolvelite.misc;

import resolvelite.typeandpopulate.DuplicateSymbolException;
import resolvelite.typeandpopulate.ScopeBuilder;
import resolvelite.typereasoning.TypeGraph;

public class HardCoded {

    public static void initMathTypeSystem(TypeGraph g, ScopeBuilder b) {
        try {
            b.addBinding("B", null, g.CLS, g.BOOLEAN);
        }
        catch (DuplicateSymbolException dse) {
            //shouldn't happen, we're first ones to add anything.
        }
    }
}
