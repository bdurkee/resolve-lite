package edu.clemson.resolve.proving;

import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.DumbMathClssftnHandler;
import edu.clemson.resolve.semantics.MathClssftn;
import edu.clemson.resolve.semantics.Quantification;

import java.util.*;
import java.util.stream.Collectors;

public class TheoremCongruenceClosureImpl {

    private final boolean isEquality;
    private final Registry m_theoremRegistry;
    private final ConjunctionOfNormalizedAtomicExpressions m_matchConj;
    private final List<NormalizedAtomicExpression> m_matchRequired;
    private final List<NormalizedAtomicExpression> m_noMatchRequired;
    public final String m_theoremString;
    private final PExp m_insertExpr;
    private final PExp m_theorem;
    protected boolean m_unneeded = false;
    private Set<String> m_all_literals;
    private boolean partMatchedisConstantEquation = false;
    protected boolean m_allowNewSymbols;
    protected String m_name;
    protected boolean m_noQuants = false;
    protected VerificationConditionCongruenceClosureImpl m_lastVC;
    protected Set<String> m_insert_qvars;
    // clear these when starting new VC
    protected List<Map<String, String>> m_bindings;
    protected Set<Map<String, String>> m_selectedBindings;

    public TheoremCongruenceClosureImpl(DumbMathClssftnHandler g, PExp entireTheorem,
                                        PExp mustMatch, PExp restOfExp, PExp toInsert,
                                        boolean enterToMatchAndBindAsEquivalentToTrue,
                                        boolean allowNewSymbols, String name) {
        m_name = name;
        m_allowNewSymbols = allowNewSymbols;
        m_theorem = entireTheorem;
        m_theoremString = entireTheorem.toString();
        isEquality = true;
        m_theoremRegistry = new Registry(g);
        m_bindings = new ArrayList<Map<String, String>>(128);
        m_selectedBindings = new HashSet<Map<String, String>>(128);
        m_matchConj = new ConjunctionOfNormalizedAtomicExpressions(m_theoremRegistry, null);
        if (mustMatch.getSubExpressions().size() > 0) {
            if (enterToMatchAndBindAsEquivalentToTrue)
                m_matchConj.addExpression(mustMatch);
            else
                m_matchConj.addFormula(mustMatch);
        }
        m_matchRequired = new ArrayList<NormalizedAtomicExpression>(m_matchConj.m_expSet.keySet());
        Collections.sort(m_matchRequired, new NormalizedAtomicExpression.numQuantsComparator());
        m_insertExpr = toInsert;
        m_insert_qvars = new HashSet<String>();
        for (PSymbol p : m_insertExpr.getQuantifiedVariables()) {
            m_insert_qvars.add(p.toString());
        }
        if (!mustMatch.equals(restOfExp)
                && restOfExp.getSubExpressions().size() > 1
                && (!mustMatch.getQuantifiedVariablesNoCache().containsAll(
                        restOfExp.getQuantifiedVariablesNoCache()) || mustMatch
                        .getSubExpressions().size() == 0)) {
            m_matchConj.addFormula(restOfExp);
        }
        m_noMatchRequired = new ArrayList<NormalizedAtomicExpression>();
        Set<String> insert_quants = new HashSet<String>();
        for (PSymbol p : toInsert.getQuantifiedVariables()) {
            insert_quants.add(p.toString());
        }
        for (NormalizedAtomicExpression n : m_matchConj.m_expSet.keySet()) {
            Map<String, Integer> ops = n.getOperatorsAsStrings(false);
            Set<String> intersection = new HashSet<String>(insert_quants);
            intersection.retainAll(ops.keySet());
            if (!m_matchRequired.contains(n) && !ops.containsKey("_g") && !intersection.isEmpty()) {
                m_noMatchRequired.add(n);
            }
        }
        Collections.sort(m_noMatchRequired, new NormalizedAtomicExpression.numQuantsComparator());
        if (m_theorem.getQuantifiedVariables().isEmpty()) {
            m_noQuants = true;
        }
    }

    public Set<String> getNonQuantifiedSymbols() {
        if (m_all_literals == null) {
            m_all_literals = m_theorem.getFreeVariables()
                    .stream()
                    .map(PSymbol::getName)
                    .collect(Collectors.toSet());

            m_all_literals.remove("=B");
            m_all_literals.remove("andB");
            m_all_literals.remove("∧B");

            m_all_literals.remove("impliesB");
            m_all_literals.remove("true");
            m_all_literals.remove("false");
            m_all_literals.remove("/=B");
            m_all_literals.remove("≠B");

            m_all_literals.remove("Empty_String");
            m_all_literals.remove("0");
            m_all_literals.remove("1");
            m_all_literals.remove("2");
            m_all_literals.remove("3");
            m_all_literals.remove("4");
            m_all_literals.remove("5");
            m_all_literals.remove("6");
            m_all_literals.remove("7");
            m_all_literals.remove("8");
            m_all_literals.remove("9");
            m_all_literals.remove("orB");
            m_all_literals.remove("∨B");
            m_all_literals.remove("+Z");
            m_all_literals.remove("+N");
        }
        return m_all_literals;
    }

    public int applyTo(VerificationConditionCongruenceClosureImpl vc,
            long endTime) {
        Set<Map<String, String>> sResults;
        m_bindings.clear();
        if (m_lastVC == null || !m_lastVC.equals(vc)) {
            m_selectedBindings.clear();
            m_lastVC = vc;
        }
        if (m_noQuants)
            return 1;
        if (m_matchRequired.size() == 0
                || ((m_allowNewSymbols && m_theorem.getQuantifiedVariables()
                        .size() == 1) && isEquality)) {
            sResults = findValidBindingsByType(vc, endTime);
        }
        else
            sResults = findValidBindings(vc, endTime);
        if (sResults == null || sResults.isEmpty())
            return 0;
        nextMap: for (Map<String, String> s : sResults) {
            if (!m_selectedBindings.contains(s)) {
                for (String k : s.keySet()) {
                    String v = s.get(k);
                    if (!m_insert_qvars.contains(v)) {
                        continue;
                    }
                    if (s.get(k).equals(""))
                        continue nextMap;
                }
                m_bindings.add(s);
            }
        }
        Collections.sort(m_bindings, new Comparator<Map<String, String>>() {

            @Override
            public int compare(Map<String, String> o1, Map<String, String> o2) {
                return calculateScore(o1) - calculateScore(o2);
            }
        });
        return m_bindings.size();
    }

    public int calculateScore(Map<String, String> bmap) {
        HashSet<String> seen = new HashSet<String>(bmap.keySet().size());
        float max = m_lastVC.getRegistry().m_indexToSymbol.size();
        float age = 0f;
        float sSz = bmap.keySet().size();
        for (String k : bmap.keySet()) {
            String rS =
                    m_lastVC.getRegistry().getRootSymbolForSymbol(bmap.get(k));
            seen.add(rS);
            if (m_lastVC.getRegistry().m_symbolToIndex.containsKey(rS)) {
                int indexVal = m_lastVC.getRegistry().getIndexForSymbol(rS);
                // Age
                age += indexVal;
            }
        }
        float diff = 1.0f - seen.size() / sSz;
        float avgAge = age / sSz;
        // these range from [0,1], lower is better
        float scaledAvgAge = avgAge / max;

        scaledAvgAge += .01;
        diff += .01;
        int r = (int) ((80f * scaledAvgAge) + (20f * diff));
        return r;
    }

    public PExpWithScore getNext() {
        if (m_noQuants && m_selectedBindings.isEmpty()) {
            m_selectedBindings.add(new HashMap<String, String>());
            return new PExpWithScore(m_insertExpr,
                    new HashMap<String, String>(), m_theorem.toString());
        }
        if (m_bindings.isEmpty())
            return null;
        HashMap<PExp, PExp> quantToLit = new HashMap<PExp, PExp>();
        Map<String, String> curBinding = m_bindings.remove(0);
        m_selectedBindings.add(curBinding);
        for (PSymbol p : m_insertExpr.getQuantifiedVariables()) {
            String thKey = p.getTopLevelOperationName();
            String thVal = "";
            if (curBinding.containsKey(thKey)) {
                thVal = curBinding.get(thKey);
            }
            else if (curBinding.containsKey(m_theoremRegistry.getRootSymbolForSymbol(thKey))) {
                thVal = curBinding.get(m_theoremRegistry.getRootSymbolForSymbol(thKey));
            }
            if (thVal.equals("")) return getNext();

            MathClssftn quanType = m_theoremRegistry.getTypeByIndex(m_theoremRegistry.getIndexForSymbol(thKey));
            PSymbol x = new PSymbol.PSymbolBuilder(thKey).mathClssfctn(quanType)
                    .quantification(Quantification.UNIVERSAL).build();
            PSymbol y = new PSymbol.PSymbolBuilder(thVal).mathClssfctn(quanType)
                    .quantification(Quantification.NONE).build();
            quantToLit.put(x, y);
        }

        PExp modifiedInsert = m_insertExpr.substitute(quantToLit);
        modifiedInsert = m_lastVC.getConjunct().find(modifiedInsert);
        // Discard s = s
        // if name is =B and left arg (index 1) == right arg (index 2)
        if ((modifiedInsert.getTopLevelOperationName().equals("=B") && modifiedInsert
                .getSubExpressions().get(1).toString().equals(
                        modifiedInsert.getSubExpressions().get(2).toString()))) {
            return getNext();
        }
        return new PExpWithScore(modifiedInsert, curBinding, m_theorem.toString());
    }

    // variables to bind are the quantified vars the quantified statement
    // and the created variables in the match conjunction
    private Map<String, String> getInitBindings() {
        HashMap<String, String> initBindings = new HashMap<String, String>();
        // Created vars. that are parents of quantified vars can be a problem later
        for (int i = 0; i < m_theoremRegistry.m_indexToSymbol.size(); ++i) {

            String curSym = m_theoremRegistry.getSymbolForIndex(i);
            Registry.Usage us = m_theoremRegistry.getUsage(curSym);
            if (us == Registry.Usage.CREATED || us == Registry.Usage.FORALL || us == Registry.Usage.HASARGS_FORALL) {
                initBindings.put(curSym, "");

            }
        }
        return initBindings;
    }

    private Set<Map<String, String>> findValidBindingsByType(
            VerificationConditionCongruenceClosureImpl vc, long endTime) {
        // Case where no match conj. is produced.
        // Example: S = Empty_String. Relevant info is only in registry.
        Set<Map<String, String>> allValidBindings = new HashSet<>();
        // x = constant?
        if (partMatchedisConstantEquation) {
            HashMap<String, String> wildToActual = new HashMap<>();
            for (String wild : m_theoremRegistry.getForAlls()) {

                String actual = m_theoremRegistry.getRootSymbolForSymbol(wild);
                // wildcard is not parent
                if (!actual.equals(wild)) {
                    wildToActual.put(wild, actual);
                }
                // wildcard is parent, bind to child
                else {
                    Set<String> ch = m_theoremRegistry.getChildren(wild);
                    // choose first non quantified symbol (they are all equal)
                    if (ch.isEmpty())
                        return null;
                    for (String c : ch) {
                        if (!m_theoremRegistry.getUsage(c).equals(
                                Registry.Usage.FORALL)
                                || !m_theoremRegistry.getUsage(c).equals(
                                        Registry.Usage.CREATED)) {
                            wildToActual.put(wild, c);
                            break;
                        }
                        return null;
                    }
                }
            }
            allValidBindings.add(wildToActual);
            return allValidBindings;

        }
        // only valid for preds other than equality
        Set<String> foralls = m_theoremRegistry.getForAlls();
        if (foralls.size() != 1) return null;
        String wild = foralls.iterator().next();
        MathClssftn t = m_theoremRegistry.getTypeByIndex(m_theoremRegistry.getIndexForSymbol(wild));

        for (String actual : vc.getRegistry().getParentsByType(t)) {
            HashMap<String, String> wildToActual = new HashMap<String, String>();
            wildToActual.put(wild, actual);
            if (!wild.equals(actual)) { // can be = with constants in theorems
                allValidBindings.add(wildToActual);
            }
        }
        return allValidBindings;

    }

    private Set<Map<String, String>> findValidBindings(VerificationConditionCongruenceClosureImpl vc, long endTime) {
        Set<Map<String, String>> results = new HashSet<>();
        Map<String, String> initBindings = getInitBindings();
        if (m_theoremRegistry.m_symbolToIndex.containsKey("_g")) {
            // each goal gets a new map with _g bound to the goal
            for (String g : vc.m_goal) {
                Map<String, String> gBinds = new HashMap<String, String>(initBindings);
                gBinds.put("_g", g);
                results.add(gBinds);
            }
        }
        else {
            results.add(initBindings);
        }
        for (NormalizedAtomicExpression e_t : m_matchRequired) {
            results = vc.getConjunct().getMatchesForOverrideSet(e_t, results);
        }
        Set<Map<String, String>> t_results;
        for (NormalizedAtomicExpression e_t : m_noMatchRequired) {
            t_results = vc.getConjunct().getMatchesForOverrideSet(e_t, results);
            if (t_results.isEmpty()) continue;
            else results.addAll(t_results);
        }
        return results;
    }

    @Override
    public String toString() {
        String r = "\n--------------------------------------\n";
        r += m_theoremString;
        r += "\nif found\n" + m_matchConj + "\ninsert\n" + m_insertExpr;
        r += "\n--------------------------------------\n";
        return r;
    }
}
