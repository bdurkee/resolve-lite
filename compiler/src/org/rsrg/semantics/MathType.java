package org.rsrg.semantics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/** The parent class of all mathematical types. */
public abstract class MathType {

    protected final DumbTypeGraph g;
    public int typeRefDepth = 0;
    protected final MathType enclosingType;

    /** Really this should only apply to instances of {@link MathNamedType}s,
     *  as those are what represent the holes that can be filled in an arbitrary
     *  {@code MathType}.
     */
    public boolean identifiesSchematicType = false;

    public MathType(@NotNull DumbTypeGraph g,
                    @Nullable MathType enclosingType) {
        this.g = g;
        this.enclosingType = enclosingType;
    }

    public DumbTypeGraph getTypeGraph() {
        return g;
    }

    public int getTypeRefDepth() {
        return typeRefDepth;
    }

    public boolean containsSchematicType() {
        for (MathType component : getComponentTypes()) {
            if (component.containsSchematicType()) return true;
        }
        return false;
    }

    @Override public boolean equals(Object o) {
        boolean result = (o instanceof MathType);
        if ( result ) {
            try {
                result = checkAlphaEquivalence(this, (MathType) o);
            }
            catch (TypeMismatchException tme) {
                result = false;
            }
        }
        return result;
    }

    public static boolean checkAlphaEquivalence(MathType t1, MathType t2)
            throws TypeMismatchException {
        if ( t1 == t2 || isEquivalentDependentType(t1, t2)) return true;
        else {
            if ( t1.getClass() != t2.getClass() ) throw new TypeMismatchException();

            List<MathType> t1Components = t1.getComponentTypes();
            List<MathType> t2Components = t2.getComponentTypes();
            if ( t1Components.size() != t2Components.size() ) throw new TypeMismatchException();

            Iterator<MathType> t1Iter = t1Components.iterator();
            Iterator<MathType> t2Iter = t2Components.iterator();
            boolean result = false ;
            while ( t1Iter.hasNext() ) {
                result = checkAlphaEquivalence(t1Iter.next(), t2Iter.next());
            }
            if (!result) throw new TypeMismatchException();
            /*Set<MathNamedType> t1FV1 = t1.getFreeNamedTypes();
            Set<MathNamedType> t2FV1 = t2.getFreeNamedTypes();
            if ( t1FV1.size() != t2FV1.size() ) throw new TypeMismatchException();

            Iterator<MathNamedType> freeVars1Iter = t1FV1.iterator();
            Iterator<MathNamedType> freeVars2Iter = t2FV1.iterator();
            //Now make sure the free var hashcodes are the same...
            boolean result = true;
            while ( freeVars1Iter.hasNext() ) {
                MathNamedType f1 = freeVars1Iter.next();
                MathNamedType f2 = freeVars2Iter.next();
                result &= f1 == f2;
            }*/
            return result;
        }
    }

    private static boolean isEquivalentDependentType(MathType t1, MathType t2) {
        boolean result = t1 instanceof MathNamedType &&
                t2 instanceof MathNamedType;
        if (result) {
            result = t1.identifiesSchematicType &&
                    t1.typeRefDepth == 0 &&
                    t2.identifiesSchematicType &&
                    t2.typeRefDepth == 0 &&
                    ((MathNamedType)t1).tag.equals(((MathNamedType)t2).tag);
        }
        return result;
    }

    public MathType getEnclosingType() {
        return enclosingType;
    }

    //TODO: eventually make this abstract -- I'm just being lazy now.
    public List<MathType> getComponentTypes() {
        return new ArrayList<>();
    }

    //TODO maybe change this map to MathNamedType -> MathNamedType, to
    //indicate this is just for names; would also be more in line with the name
    //of this method...
    public abstract MathType withVariablesSubstituted(
            Map<MathType, MathType> substitutions);

}
