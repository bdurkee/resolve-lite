package edu.clemson.resolve.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The parent class of all mathematical types.
 */
public abstract class MathClassification {

    protected final DumbTypeGraph g;
    public int typeRefDepth = 0;
    public MathClassification enclosingClassification;

    /**
     * Really this should only apply to instances of {@link MathNamedClassification}s,
     * as those are what represent the holes that can be filled in an arbitrary
     * {@code MathClassification}.
     */
    public boolean identifiesSchematicType = false;

    public MathClassification(@NotNull DumbTypeGraph g,
                              @Nullable MathClassification enclosingClassification) {
        this.g = g;
        this.enclosingClassification = enclosingClassification;
    }

    public DumbTypeGraph getTypeGraph() {
        return g;
    }

    public int getTypeRefDepth() {
        return typeRefDepth;
    }

    public boolean containsSchematicType() {
        for (MathClassification component : getComponentTypes()) {
            if (component.containsSchematicType()) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof MathClassification);
        if (result) {
            try {
                result = checkAlphaEquivalence(this, (MathClassification) o);
            } catch (TypeMismatchException tme) {
                result = false;
            }
        }
        return result;
    }

    public static boolean checkAlphaEquivalence(MathClassification t1, MathClassification t2)
            throws TypeMismatchException {
        if (t1 == t2 || isEquivalentDependentType(t1, t2)) return true;
        else {
            if (t1.getClass() != t2.getClass())
                throw new TypeMismatchException();

            List<MathClassification> t1Components = t1.getComponentTypes();
            List<MathClassification> t2Components = t2.getComponentTypes();
            if (t1Components.size() != t2Components.size())
                throw new TypeMismatchException();

            Iterator<MathClassification> t1Iter = t1Components.iterator();
            Iterator<MathClassification> t2Iter = t2Components.iterator();
            boolean result = false;
            while (t1Iter.hasNext()) {
                result = checkAlphaEquivalence(t1Iter.next(), t2Iter.next());
            }
            if (!result) throw new TypeMismatchException();
            /*Set<MathNamedClassification> t1FV1 = t1.getFreeNamedTypes();
            Set<MathNamedClassification> t2FV1 = t2.getFreeNamedTypes();
            if ( t1FV1.size() != t2FV1.size() ) throw new TypeMismatchException();

            Iterator<MathNamedClassification> freeVars1Iter = t1FV1.iterator();
            Iterator<MathNamedClassification> freeVars2Iter = t2FV1.iterator();
            //Now make sure the free var hashcodes are the same...
            boolean result = true;
            while ( freeVars1Iter.hasNext() ) {
                MathNamedClassification f1 = freeVars1Iter.next();
                MathNamedClassification f2 = freeVars2Iter.next();
                result &= f1 == f2;
            }*/
            return result;
        }
    }

    private static boolean isEquivalentDependentType(MathClassification t1, MathClassification t2) {
        boolean result = t1 instanceof MathNamedClassification &&
                t2 instanceof MathNamedClassification;
        if (result) {
            result = t1.identifiesSchematicType &&
                    t1.typeRefDepth == 0 &&
                    t2.identifiesSchematicType &&
                    t2.typeRefDepth == 0 &&
                    ((MathNamedClassification) t1).tag.equals(((MathNamedClassification) t2).tag);
        }
        return result;
    }

    public MathClassification getEnclosingClassification() {
        return enclosingClassification;
    }

    //TODO: eventually make this abstract -- I'm just being lazy now.
    public List<MathClassification> getComponentTypes() {
        return new ArrayList<>();
    }

    //TODO maybe change this map to MathNamedClassification -> MathNamedClassification, to
    //indicate this is just for names; would also be more in line with the name
    //of this method...
    public abstract MathClassification withVariablesSubstituted(
            Map<String, MathClassification> substitutions);

}
