package org.rsrg.semantics;

import java.util.*;

/**
 * The parent class of all mathematical types.
 */
public abstract class MTType {

    protected final TypeGraph myTypeGraph;

    private final Set<Object> myKnownAlphaEquivalencies = new HashSet<>();
    private final Map<MTType, Map<String, MTType>> myKnownSyntacticSubtypeBindings =
            new HashMap<>();

    /**
     * Allows us to detect if we're getting into an equals-loop.
     */
    private int myEqualsDepth = 0;

    public MTType(TypeGraph typeGraph) {
        myTypeGraph = typeGraph;
    }

    public TypeGraph getTypeGraph() {
        return myTypeGraph;
    }

    public abstract void accept(TypeVisitor v);

    public abstract void acceptOpen(TypeVisitor v);

    public abstract void acceptClose(TypeVisitor v);

    public abstract List<MTType> getComponentTypes();

    public abstract MTType withComponentReplaced(int index, MTType newType);

    public MTType withComponentsReplaced(Map<Integer, MTType> newTypes) {

        MTType target = this;
        for (Map.Entry<Integer, MTType> entry : newTypes.entrySet()) {
            target =
                    target.withComponentReplaced(entry.getKey(),
                            entry.getValue());
        }
        return target;
    }

    @Override public final boolean equals(Object o) {
        boolean result;

        if ( this == o ) {
            result = true;
        }
        else {
            result = o.toString().equals(this.toString());
        }
        return result;
    }

    public final Map<String, MTType> getSyntacticSubtypeBindings(MTType o)
            throws NoSolutionException {

        Map<String, MTType> result;

        if ( myKnownSyntacticSubtypeBindings.containsKey(o) ) {
            result = myKnownSyntacticSubtypeBindings.get(o);
        }
        else {
            SyntacticSubtypeChecker checker =
                    new SyntacticSubtypeChecker(myTypeGraph);

            try {
                checker.visit(this, o);
            }
            catch (RuntimeException e) {

                Throwable cause = e;
                while (cause != null
                        && !(cause instanceof TypeMismatchException)) {
                    cause = cause.getCause();
                }

                if ( cause == null ) {
                    throw e;
                }

                throw NoSolutionException.INSTANCE;
            }

            result = Collections.unmodifiableMap(checker.getBindings());
            myKnownSyntacticSubtypeBindings.put(o, result);
        }

        return result;
    }

    public final boolean isSubtypeOf(MTType o) {
        return myTypeGraph.isSubtype(this, o);
    }

    public final boolean isSyntacticSubtypeOf(MTType o) {

        boolean result;

        try {
            getSyntacticSubtypeBindings(o);
            result = true;
        }
        catch (NoSolutionException e) {
            result = false;
        }

        return result;
    }

    public final boolean isBoolean() {
        return (myTypeGraph.BOOLEAN == this);
    }

    public final boolean alphaEquivalentTo(MTType t) {
        return this.equals(t);
    }

    public final MTType getCopyWithVariablesSubstituted(
            Map<String, MTType> substitutions) {
        VariableReplacingVisitor renamer =
                new VariableReplacingVisitor(substitutions);
        accept(renamer);
        return renamer.getFinalExpression();
    }

    /*public Map<String, MTType> bindTo(MTType o, Scope context)
            throws BindingException {

        BindingVisitor bind = new BindingVisitor(myTypeGraph, context);
        bind.visit(this, o);

        if ( !bind.binds() ) {
            throw new BindingException(this, o);
        }

        return bind.getBindings();
    }

    public Map<String, MTType> bindTo(MTType o, Map<String, MTType> context)
            throws BindingException {

        BindingVisitor bind = new BindingVisitor(myTypeGraph, context);
        bind.visit(this, o);

        if ( !bind.binds() ) {
            throw new BindingException(this, o);
        }

        return bind.getBindings();
    }*/

    public Map<String, MTType>
            bindTo(MTType template, Map<String, MTType> thisContext,
                    Map<String, MTType> templateContext)
                    throws BindingException {

        BindingVisitor bind =
                new BindingVisitor(myTypeGraph, thisContext, templateContext);
        bind.visit(this, template);

        if ( !bind.binds() ) {
            throw new BindingException(this, template);
        }

        return bind.getBindings();
    }

    public MTType getType() {
        //TODO : Each MTType should really contain it's declared type.  I.e.,
        //       if I say "Definition X : Set", I should store that X is
        //       of type Set someplace.  That's not currently available, so for
        //       the moment we say that all types are of type MType, the parent
        //       type of all types.
        return myTypeGraph.MTYPE;
    }

    /**
     * Returns the object-reference hash.
     */
    public final int objectReferenceHashCode() {
        return super.hashCode();
    }

    @Override public final int hashCode() {
        return getHashCode();
    }

    /**
     * This is just a template method to force all concrete subclasses of
     * {@code MTType} to implement {@code hashCode()}, as the type resolution
     * algorithm depends on it being implemented sensibly.
     * 
     * @return A hashcode consistent with {@code equals()} and thus
     *         alpha-equivalency.
     */
    public abstract int getHashCode();

    /**
     * Indicates that this type is known to contain only elements that
     * are themselves types. Practically, this answers the question, "can
     * an instance of this type itself be used as a type?"
     */
    public boolean isKnownToContainOnlyMTypes() {
        return false;
    }

    /**
     * Indicates that every instance of this type is itself known to contain
     * only elements that are types. Practically, this answers the question,
     * "if a function returns an instance of this type, can that instance itself
     * be said to contain only types?"
     */
    public boolean membersKnownToContainOnlyMTypes() {
        return false;
    }

    private class MTTypeObjectHashWrapper {

        public final MTType type;

        public MTTypeObjectHashWrapper(MTType t) {
            type = t;
        }

        @Override public boolean equals(Object o) {
            return type.equals(o);
        }

        @Override public int hashCode() {
            return type.objectReferenceHashCode();
        }
    }
}
