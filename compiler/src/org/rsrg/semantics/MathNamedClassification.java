package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** Represents a type that is simply a named reference to some bound variable.
 *  For example, in {@code BigUnion t : MType}{t}}, the second {@code t} is a
 *  named type.
 */
public class MathNamedClassification extends MathClassification {

    public String tag;

    public MathNamedClassification(@NotNull DumbTypeGraph g, String tag,
                                   int typeDepth,
                                   @Nullable MathClassification enclosingType) {
        super(g, enclosingType);
        this.tag = tag;
        this.typeRefDepth = typeDepth;
        this.identifiesSchematicType = false;
    }

    @Override public boolean containsSchematicType() {
        return identifiesSchematicType;
    }

    @Override public MathClassification withVariablesSubstituted(
            Map<String, MathClassification> substitutions) {
        MathClassification result = substitutions.get(this.tag);
        return result == null ? this : result;
    }

    @Override public String toString() {
        return tag;
    }

}