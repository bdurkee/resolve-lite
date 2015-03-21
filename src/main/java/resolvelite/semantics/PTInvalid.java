package resolvelite.semantics;

import resolvelite.typereasoning.TypeGraph;

public class PTInvalid extends PTType {

    public PTInvalid(TypeGraph g) {
        super(g);
    }

    @Override
    public MTType toMath() {
        throw new UnsupportedOperationException(
                "invalid type has no math equivalent");
    }
}
