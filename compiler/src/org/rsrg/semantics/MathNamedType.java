package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Represents a type that is simply a named reference to some bound variable.
 *  For example, in {@code BigUnion t : MType}{t}}, the second {@code t} is a
 *  named type.
 */
public class MathNamedType extends MathType {

    public String tag;

    public MathNamedType(@NotNull DumbTypeGraph g, String tag,
                         int typeDepth,
                         @Nullable MathType enclosingType) {
        super(g, enclosingType);
        this.tag = tag;
        this.typeRefDepth = typeDepth;
    }

    @Override public boolean containsSchematicType() {
        return identifiesSchematicType;
    }

    @Override public MathType withVariablesSubstituted(
            Map<MathType, MathType> substitutions) {
        MathType result = substitutions.get(this);
        return result == null ? this : result;
    }

    @Override public String toString() {
        return tag;
    }

}