package edu.clemson.resolve.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Represents a type that is simply a named reference to some bound variable.
 * For example, in {@code BigUnion t : MType}{t}}, the second {@code t} is a
 * named type.
 */
public class MathNamedClssftn extends MathClssftn {

    public String tag;

    public MathNamedClssftn(@NotNull DumbMathClssftnHandler g,
                            String tag,
                            int typeDepth,
                            @Nullable MathClssftn enclosingType) {
        super(g, enclosingType);
        this.tag = tag;
        this.typeRefDepth = typeDepth;
        this.identifiesSchematicType = false;
    }

    @Override
    public boolean containsSchematicType() {
        return identifiesSchematicType;
    }

    @Override
    public MathClssftn withVariablesSubstituted(Map<String, MathClssftn> substitutions) {
        MathClssftn result = substitutions.get(this.tag);
        return result == null ? this : result;
    }

    @Override
    public String toString() {
        return tag;
    }

}