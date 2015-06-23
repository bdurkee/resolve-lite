package org.rsrg.semantics;

import edu.clemson.resolve.typereasoning.TypeGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * A syntactic subtype refers to a type that can be demonstrated
 * as a (non-strict) subtype of some other type using only syntactic
 * information. Specifically, without recourse to type theorems. The syntactic
 * subtype relationship thus completely captures all hard-coded type
 * relationship information.
 * 
 * This class implements a check for a syntactic subtype relationship as a
 * symmetric visitor. To check if {@code t1} is a syntactic subtype of
 * {@code t2}, use code like this:
 * 
 * {@pre
 * SyntacticSubtypeChecker checker = new SyntacticSubtypeChecker(typeGraph); try
 * checker.visit(t1, t2);
 * //Stuff to do if t1 is a syntactic subtype of t2 * } catch
 * (IllegalArgumentException e) {
 * TypeMismatchException mismatch = (TypeMismatchException) e.getCause();
 * //Stuff to do if t1 is NOT a syntactic subtype of t2
 * }}
 * 
 * As shown, the {@code visit()} method of a {@code SyntacticSubtypeChecker}
 * will exit normally if two types have a syntactic subtype relationship, or
 * throw an {@code IllegalArgumentException} if they do not. The
 * {@code IllegalArgumentException} will wrap a {@link TypeMismatchException}
 * describing the specific problem.
 * 
 * As the checker descends the type trees in parallel, it keeps a record of
 * actual component types from {@code t1} that were matched with some
 * quantified type-variable from {@code t2}. After a successful check,
 * this record can be accessed via {@code getBindings()}.
 * 
 * At this time, the following syntactic relationships are recognized as
 * forming a syntactic subtype relationship:
 * 
 * <ul>
 * <li>Any type is a syntactic subtype of both <strong>MType</strong> and
 * <strong>Entity</strong> (which is a superset of <strong>MType</strong>).</li>
 * <li><strong>Empty_Set</strong> is a subtype of all types, including itself.</li>
 * <li>Any type is a syntactic subtype of itself or a type that is alpha
 * equivalent to itself.</li>
 * <li>{@code t1} is a syntactic subtype of {@code t2} if
 * {@code BigUnion unique_var_name_1 : MType, ... unique_var_name_n : MType}{t1}
 * } (for some {@code n > 0}, where each {@code unique_var-name_1} does not
 * appear in {@code t1}) is a syntactic subtype of {@code t2}.</li>
 * <li>
 * 
 * <pre>
 * BigUnion{t1 : (T1 : Power(MType)),
 *          t2 : (T2 : Power(MType)),
 *                ...
 *          tn : (Tn : Power(MType))}
 *         {t_type_valued_expression}
 * </pre>
 * 
 * Is a syntactic subtype of:
 * 
 * <pre>
 * BigUnion{r1 : (R1 : Power(MType)),
 *          r2 : (R2 : Power(MType)),
 *                ...
 *          rk : (rk : Power(MType))}
 *         {r_type_valued_expression}
 * </pre>
 * 
 * If {@code n &lt k}and there is some valuation of a (non-strict) subset of the
 * {@code r}s and some restriction (to syntactic subtypes) of the {@code R}s not
 * associated with {@code r}s in the valuation subset such that
 * {@code r_type_valued_expression}> becomes alpha-equivalent to
 * {@code t_type_valued_expression}.</li>
 * 
 * <strong>TODO:</strong> Currently we do not deal correctly with types where
 * the same quantified variable appears multiple times in the template, e.g., "
 * <code>BigUnion{t : MType}{t union t}</code>". This is coded defensively and
 * will throw a <code>RuntimeException</code> if it occurs.
 */
public class SyntacticSubtypeChecker extends SymmetricBoundVariableVisitor {

    private static final IllegalArgumentException MISMATCH =
            new IllegalArgumentException(TypeMismatchException.INSTANCE);

    private Map<String, MTType> myBindings = new HashMap<String, MTType>();

    private final TypeGraph myTypeGraph;

    private HashMap<String, MTType> myPromotedVariablesWorkingSpace;

    public SyntacticSubtypeChecker(TypeGraph g) {
        myTypeGraph = g;
    }

    public SyntacticSubtypeChecker(TypeGraph g, Scope context1) {
        super(context1);
        myTypeGraph = g;
    }

    public SyntacticSubtypeChecker(TypeGraph g, Map<String, MTType> context1) {
        super(context1);
        myTypeGraph = g;
    }

    public Map<String, MTType> getBindings() {
        return myBindings;
    }

    /**
     * Resets a checker so that it is prepared to check a new pair of types.
     */
    @Override public void reset() {
        super.reset();
        myBindings.clear();
    }

    @Override public boolean beginMTFunctionApplication(
            MTFunctionApplication t1, MTFunctionApplication t2) {
        if ( !t1.getName().equals(t2.getName()) ) {
            throw MISMATCH;
        }
        return true;
    }

    @Override public boolean beginMTType(MTType t1, MTType t2) {
        //Alpha-equivalent types are definitely syntactic subtypes.  No need
        //to descend
        return !t1.equals(t2);
    }

    @Override public boolean beginMTNamed(MTNamed t1, MTNamed t2) {

        if ( !t1.name.equals(t2.name) ) {

            if ( getInnermostBinding2(((MTNamed) t2).name).equals(
                    myTypeGraph.MTYPE) ) {
                bind(((MTNamed) t2).name, t1);
            }
            else {
                MTType t1DeclaredType = t1;
                MTType t2DeclaredType = t2;
                try {
                    t1DeclaredType = getInnermostBinding1(t1.name);
                }
                catch (NoSuchElementException nsee) {

                }

                try {
                    t2DeclaredType = getInnermostBinding2(t2.name);
                }
                catch (NoSuchElementException nsee) {

                }

                if ( t1DeclaredType == t1 && t2DeclaredType == t2 ) {
                    //We have no information on these named types, but they don't
                    //share a name, so...
                    throw MISMATCH;
                }

                if ( !haveAxiomaticSubtypeRelationship(t1DeclaredType,
                        t2DeclaredType) ) {
                    //This is fine if the declared type of t1 is a syntactic subtype
                    //of the declared type of t2
                    visit(t1DeclaredType, t2DeclaredType);
                }
            }
        }

        return true; //Keep searching siblings
    }

    private void bind(String name, MTType type) {
        if ( myBindings.containsKey(name) ) {
            throw new RuntimeException("Duplicate quantified variable name: "
                    + name);
        }

        myBindings.put(name, type);
    }

    /*@Override
    public boolean beginMTSetRestriction(MTSetRestriction t1,
                                         MTSetRestriction t2) {

        //TODO:
        //For the moment, there's no obvious way to do this.  We'll just say no
        //set restriction can be a syntactic subtype of any other.
        throw MISMATCH;
    }*/

    @Override public boolean beginMTProper(MTProper t1, MTProper t2) {
        if ( !(t1 == t2 || haveAxiomaticSubtypeRelationship(t1, t2)) ) {
            throw MISMATCH;
        }
        return true;
    }

    @Override public boolean mismatch(MTType t1, MTType t2) {

        //Note it's possible that t1 and t2 could both be MTBigUnion, even
        //though we're in mismatch() because they could have a different number
        //of quantified subtypes.
        if ( t2 instanceof MTBigUnion && !(t1 instanceof MTBigUnion) ) {
            //This may be ok, since we can wrap any expression in a trivial
            //big union
            MTBigUnion t2AsMTBigUnion = (MTBigUnion) t2;
            int quantifiedVariableCount =
                    t2AsMTBigUnion.getQuantifiedVariables().size();

            t1 = new MTBigUnion(t1.getTypeGraph(), quantifiedVariableCount, t1);

            visit(t1, t2);
        }
        else if ( t2 instanceof MTNamed
                && getInnermostBinding2(((MTNamed) t2).name).equals(
                        myTypeGraph.MTYPE) ) {

            bind(((MTNamed) t2).name, t1);
        }
        else if ( haveAxiomaticSubtypeRelationship(t1, t2) ) {
            //We're a syntactic subtype, so we don't need to do anything
        }
        else {
            //Otherwise, there's no way to continue, so we bomb
            throw MISMATCH;
        }

        return true; //Keep searching siblings
    }

    private boolean haveAxiomaticSubtypeRelationship(MTType subtype,
            MTType supertype) {

        //Respectively, here:  EMPTY_SET is a subtype of everything, everything
        //is a subtype of CLS, and everything is a subtype of ENTITY.

        return subtype.equals(myTypeGraph.EMPTY_SET)
                || supertype.equals(myTypeGraph.MTYPE)
                || supertype.equals(myTypeGraph.ENTITY);
    }
}