package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.semantics.MathClssftn;
import org.jetbrains.annotations.NotNull;

//Going to serve as a class for general exprs that can be names.
//Thinking now that PSymbol and PSelector will extend this... And NPV will take this as its parameter...
public abstract class PNameLikeElement extends PExp {

    public PNameLikeElement(@NotNull HashDuple hashes, @NotNull MathClssftn type) {
        super(hashes, type);
    }

    public abstract PNameLikeElement withPrimeMarkAdded();

    @NotNull//pselector will return everything, dots and all.
    public abstract String getName();
}
