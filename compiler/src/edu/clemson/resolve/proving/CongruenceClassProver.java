package edu.clemson.resolve.proving;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.compiler.ErrorKind;
import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.semantics.*;
import edu.clemson.resolve.semantics.query.MathSymbolQuery;
import edu.clemson.resolve.semantics.query.NameQuery;
import edu.clemson.resolve.semantics.query.SymbolTypeQuery;
import edu.clemson.resolve.semantics.symbol.MathClssftnWrappingSymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;
import edu.clemson.resolve.semantics.symbol.TheoremSymbol;
import edu.clemson.resolve.vcgen.VC;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class CongruenceClassProver {

    private static final long DEFAULT_TIMEOUT = 5000;
    private static final int DEFAULT_TRIES = -1;
    private static final boolean SHOW_RESULTS_IF_NOT_PROVED = true;

    private final List<VerificationConditionCongruenceClosureImpl> m_ccVCs = new ArrayList<>();
    private final List<TheoremCongruenceClosureImpl> m_theorems = new ArrayList<>();
    private final Set<String> m_nonQuantifiedTheoremSymbols = new HashSet<>();
    private final Set<TheoremCongruenceClosureImpl> m_smallEndEquations = new HashSet<>();

    @NotNull
    private final ModuleScopeBuilder m_scope;
    @NotNull
    private final DumbMathClssftnHandler m_typeGraph;

    private String m_results;
    private boolean printVCEachStep = false;
    private long timeout;
    private long totalTime = 0;

    private final int numTriesBeforeQuitting;
    private final RESOLVECompiler compiler;
    private final AnnotatedModule tr;

    public CongruenceClassProver(@NotNull RESOLVECompiler compiler,
                                 @NotNull AnnotatedModule target,
                                 @NotNull DumbMathClssftnHandler g,
                                 @NotNull List<VC> vcs) {
        this.compiler = compiler;
        this.timeout = compiler.timeout != null ? Long.parseLong(compiler.timeout) : DEFAULT_TIMEOUT;
        this.numTriesBeforeQuitting = compiler.tries != null ? Integer.parseInt(compiler.tries) : DEFAULT_TRIES;
        this.totalTime = System.currentTimeMillis();
        this.m_typeGraph = g;
        try {
            this.m_scope = compiler.symbolTable.getModuleScope(target.getModuleIdentifier());
        } catch (NoSuchModuleException e) {
            throw new RuntimeException("prover couldn't obtain modulescope for whatever reason..");
        }
        this.tr = target;

        MathClssftn z = null;
        MathClssftn n = null;
        try {
            z = getMathSymbol(m_scope, "Z").getClassification();
            n = getMathSymbol(m_scope, "N").getClassification();
        } catch (SymbolTableException e) {
            compiler.info("warning: could not find some fundamental base sorts/classifications " +
                    "used by the prover: N and/or Z");
        }
        List<VC> preprocessedVcs = preprocessVCs(vcs);
        for (VC vc : preprocessedVcs) {
            m_ccVCs.add(new VerificationConditionCongruenceClosureImpl(g, vc, z, n));
        }
        List<TheoremSymbol> theoremSymbols = new ArrayList<>();
        try {
            theoremSymbols.addAll(
                    m_scope.query(new SymbolTypeQuery<TheoremSymbol>(TheoremSymbol.class,
                            MathSymbolTable.ImportStrategy.IMPORT_RECURSIVE,
                            MathSymbolTable.FacilityStrategy.FACILITY_IGNORE)));
        } catch (NoSuchModuleException|UnexpectedSymbolException e) {
        }
        for (TheoremSymbol e : theoremSymbols) {
            PExp assertion = Utilities.replacePExp(e.getAssertion(), m_typeGraph, z, n);
            String eName = e.getName();
            if (assertion.getTopLevelOperationName().equals("=B") && assertion.getQuantifiedVariables().size() > 0) {
                addEqualityTheorem(true, assertion, eName + "_left"); // match left
                addEqualityTheorem(false, assertion, eName + "_right"); // match right
                //m_theorems.add(new TheoremCongruenceClosureImpl(g, assertion, assertion, assertion, false,
                //false, eName + "_whole")); // match whole*/
            }
            else {
                TheoremCongruenceClosureImpl t;
                if (assertion.getTopLevelOperationName().equals("impliesB")) {

                    //O.k. it seems we can safely assume the assertion (at the top level at least) will be an instance
                    //of a function application (PApply)
                    addGoalSearchingTheorem((PApply)assertion, eName);
                    t =
                            new TheoremCongruenceClosureImpl(g, assertion,
                                    assertion.getSubExpressions().get(0),
                                    assertion.getSubExpressions().get(1),
                                    assertion.getSubExpressions().get(1), true,
                                    false, eName);
                }
                else {
                    t =
                            new TheoremCongruenceClosureImpl(g, assertion,
                                    assertion, assertion, assertion, false,
                                    false, eName);
                }
                m_theorems.add(t);
                m_nonQuantifiedTheoremSymbols.addAll(t.getNonQuantifiedSymbols());
                //addContrapositive(assertion, eName);
            }
        }
        if (n != null && z != null) {
            sumConversion(n, z);
        }
        m_results = "";
    }

    private List<VC> preprocessVCs(List<VC> vcs) {
        List<VC> result = new ArrayList<>();
        for (VC vc : vcs) {
            PExp newAntecedent = vc.getAntecedent();
            PExp newConsequent = vc.getConsequent();

            // make every PExp a PSymbol
            //vc.convertAllToPsymbols(m_typeGraph);
            //result.add()
        }
        result.addAll(vcs); //TODO: Not doing the conversions now. (I don't use lambdas right now, etc)
        return result;
    }

    @Nullable
    private static MathClssftnWrappingSymbol getMathSymbol(@NotNull Scope s, @NotNull String name)
            throws SymbolTableException {
        return s.queryForOne(new MathSymbolQuery(null, name));
    }

    private void addEqualityTheorem(boolean matchLeft, PExp theorem, String thName) {
        PExp lhs, rhs;
        List<? extends PExp> subExps = theorem.getSubExpressions();
        if (theorem instanceof PApply) {
            subExps = ((PApply) theorem).getArguments();
        }
        if (matchLeft) {
            lhs = subExps.get(0);
            rhs = subExps.get(1);
        }
        else {
            lhs = subExps.get(1);
            rhs = subExps.get(0);
        }

        //if lhs is a PApply lhs.getSubexpressions will be at least size 1 (for function name exp portion) + at least
        //one for an argument. If there were zero args, it wouldn't have been a PApply. It would've been built as a
        //PSymbol. So in other words, I think this logic will still hold up.
        if (lhs.getSubExpressions().size() > 0 || rhs.getSubExpressions().size() > 0) {
            TheoremCongruenceClosureImpl t =
                    new TheoremCongruenceClosureImpl(m_typeGraph, theorem, lhs, rhs, theorem, false, false, thName);
            m_theorems.add(t);
            if (lhs.getSymbolNames().size() < rhs.getSymbolNames().size()) {
                m_smallEndEquations.add(t);
            }
        }
    }

    // forall x. p(x) -> q(x) to
    // forall x,y,_g.((q(x) = _g) )
    //              -> (_g = (p(x) or _g))
    // the idea is to find q(x) = g, then add all p(x,y) we can find to goal
    private void addGoalSearchingTheorem(PApply theorem, String name) {
        // search method will do a search for each current goal, replacing _g with goal in the binding map

        //create a goal symbol
        PSymbol goal = new PSymbol.PSymbolBuilder("_g")
                .quantification(Quantification.UNIVERSAL)
                .mathClssfctn(m_typeGraph.BOOLEAN)
                .build();

        //create q(x) = _g
        PApply ant = new PApply.PApplyBuilder(Utilities.buildEqBName(m_typeGraph))
                .arguments(theorem.getArguments().get(1), goal)
                .applicationType(m_typeGraph.BOOLEAN)
                .build();

        //create p(x) or _g
        PApply pOrG = new PApply.PApplyBuilder(Utilities.buildOrBName(m_typeGraph))
                .arguments(theorem.getSubExpressions().get(0), goal)
                .applicationType(m_typeGraph.BOOLEAN)
                .build();

        //create (p(x) or _g) = _g
        PApply consequent = new PApply.PApplyBuilder(Utilities.buildEqBName(m_typeGraph))
                .arguments(pOrG, goal)
                .applicationType(m_typeGraph.BOOLEAN)
                .build();

        TheoremCongruenceClosureImpl t =
                new TheoremCongruenceClosureImpl(m_typeGraph, theorem, ant,
                        consequent, consequent, true, false, name + "_goalSearch");
        m_theorems.add(t);
    }

    // Temporarily coding conversion theorem for natural / integer addition
    // forall x,y:N, +N(x,y) = +Z(x,y) match left only
    void sumConversion(MathClssftn n, MathClssftn z) {

        PSymbol x = new PSymbol.PSymbolBuilder("x")
                .quantification(Quantification.UNIVERSAL)
                .mathClssfctn(n)
                .build();

        PSymbol y = new PSymbol.PSymbolBuilder("y")
                .quantification(Quantification.UNIVERSAL)
                .mathClssfctn(n)
                .build();

        PSymbol nPlusName = new PSymbol.PSymbolBuilder("+N")
                .mathClssfctn(new MathFunctionClssftn(m_typeGraph, n, n, n))
                .build();
        //+N(x,y)
        PApply nPlusApp = new PApply.PApplyBuilder(nPlusName)
                .arguments(x, y)
                .applicationType(n)
                .build();

        PSymbol zPlusName = new PSymbol.PSymbolBuilder("+Z")
                .mathClssfctn(new MathFunctionClssftn(m_typeGraph, z, z, z))
                .build();
        //+Z(x,y)
        PApply zPlusApp = new PApply.PApplyBuilder(zPlusName)
                .arguments(x, y)
                .applicationType(z)
                .build();

        //+N(x,y) =B +Z(x,y)
        PApply eq = new PApply.PApplyBuilder(Utilities.buildEqBName(m_typeGraph))
                .arguments(nPlusApp, zPlusApp)
                .applicationType(m_typeGraph.BOOLEAN)
                .build();
        String name = "Integer / Natural Sum Conversion";
        addEqualityTheorem(true, eq, name + "_left");
        addEqualityTheorem(false, eq, name + "_right");
    }

    //START

    public void start() throws IOException {

        String summary = "";
        int i = 0;
        int numUnproved = 0;
        for (VerificationConditionCongruenceClosureImpl vcc : m_ccVCs) {
            //printVCEachStep = true;
            //if (!vcc.m_name.equals("0_2")) continue;
            long startTime = System.nanoTime();
            String whyQuit = "";
            // Skip proof loop
            if (numTriesBeforeQuitting >= 0 && numUnproved >= numTriesBeforeQuitting) {
                summary += vcc.m_name + " skipped\n";
                ++i;
                continue;
            }
            VerificationConditionCongruenceClosureImpl.STATUS proved = prove(vcc);
            if (proved.equals(VerificationConditionCongruenceClosureImpl.STATUS.PROVED)) {
                whyQuit += " Proved ";
            }
            else if (proved.equals(VerificationConditionCongruenceClosureImpl.STATUS.FALSE_ASSUMPTION)) {
                whyQuit += " Proved (Assumption(s) false) ";
            }
            else if (proved.equals(VerificationConditionCongruenceClosureImpl.STATUS.STILL_EVALUATING)) {
                whyQuit += " Out of theorems, or timed out ";
                numUnproved++;
            }
            else {
                whyQuit += " Goal false "; // this isn't currently reachable
            }

            long endTime = System.nanoTime();
            long delayNS = endTime - startTime;
            long delayMS = TimeUnit.MILLISECONDS.convert(delayNS, TimeUnit.NANOSECONDS);
            summary += vcc.m_name + whyQuit + " time: " + delayMS + " ms\n";
            i++;

        }
        totalTime = System.currentTimeMillis() - totalTime;
        summary += "Elapsed time from construction: " + totalTime + " ms" + "\n";
        String div = divLine("Summary");
        summary = div + summary + div;
        outputProofFile();
    }

    private String divLine(String label) {
        if (label.length() > 78) {
            label = label.substring(0, 77);
        }
        label = " " + label + " ";
        char[] div = new char[80];
        Arrays.fill(div, '=');
        int start = 40 - label.length() / 2;
        for (int i = start, j = 0; j < label.length(); ++i, ++j) {
            div[i] = label.charAt(j);
        }
        return new String(div) + "\n";
    }

    /* while not proved do
        rank theorems
            while top rank below threshold score do
                apply top rank if not in exclusion list
                insert top rank
                add inserted expression to exclusion list
                choose new top rank
     */
    protected VerificationConditionCongruenceClosureImpl.STATUS prove(
            VerificationConditionCongruenceClosureImpl vcc) {
        ArrayList<TheoremCongruenceClosureImpl> theoremsForThisVC = new ArrayList<>();
        theoremsForThisVC.addAll(m_theorems);
        long startTime = System.currentTimeMillis();
        long endTime = timeout + startTime;
        Map<String, Integer> theoremAppliedCount = new HashMap<>();
        VerificationConditionCongruenceClosureImpl.STATUS status = vcc.isProved();
        String div = divLine(vcc.m_name);
        String theseResults = div + ("Before application of theorems: " + vcc + "\n");

        int iteration = 0;
        // ++++++ Create new PQ for instantiated theorems
        chooseNewTheorem: while (status
                .equals(VerificationConditionCongruenceClosureImpl.STATUS.STILL_EVALUATING)
                && System.currentTimeMillis() <= endTime) {
            long time_at_theorem_pq_creation = System.currentTimeMillis();
            // ++++++ Creates new PQ with all the theorems
            TheoremPrioritizer rankedTheorems =
                    new TheoremPrioritizer(theoremsForThisVC,
                            theoremAppliedCount, vcc,
                            m_nonQuantifiedTheoremSymbols, m_smallEndEquations);
            int max_Theorems_to_choose = 1;
            int num_Theorems_chosen = 0;
            while (!rankedTheorems.m_pQueue.isEmpty()
                    && status
                    .equals(VerificationConditionCongruenceClosureImpl.STATUS.STILL_EVALUATING)
                    && (num_Theorems_chosen < max_Theorems_to_choose || rankedTheorems.m_pQueue
                    .peek().m_score <= 1)) {
                // +++++++ Chooses top of uninstantiated theorem PQ
                long time_at_selection = System.currentTimeMillis();
                int theoremScore = rankedTheorems.m_pQueue.peek().m_score;
                TheoremCongruenceClosureImpl cur = rankedTheorems.poll();
                // Mark as used
                int count = 0;
                if (theoremAppliedCount.containsKey(cur.m_name)) count = theoremAppliedCount.get(cur.m_name);
                theoremAppliedCount.put(cur.m_name, ++count);
                // We are using it, even if it makes no difference
                int instThMatches = cur.applyTo(vcc, endTime);
                PExpWithScore tMatch = cur.getNext();
                if (tMatch != null) {
                    String substitutionMade = "";
                    int innerctr = 0;
                    long t2 = System.currentTimeMillis();
                    substitutionMade =
                            vcc.getConjunct().addExpressionAndTrackChanges(
                                    tMatch.m_theorem, endTime,
                                    tMatch.m_theoremDefinitionString);
                    if (cur.m_noQuants) {
                        theoremsForThisVC.remove(cur);
                    }
                    if (!substitutionMade.equals("")) {
                        long curTime = System.currentTimeMillis();
                        theseResults +=
                                "Iter:"
                                        + iteration++
                                        + "."
                                        + (innerctr++)
                                        + " Iter Time: "
                                        + (curTime - time_at_theorem_pq_creation)
                                        + " Search Time for this theorem: "
                                        + (curTime - time_at_selection)
                                        + " Elapsed Time: "
                                        + (curTime - startTime) + "\n["
                                        + theoremScore + "]" + cur.m_name
                                        + "\n" + tMatch.toString() + "\t"
                                        + substitutionMade + "\n\n";
                        if (printVCEachStep) theseResults += vcc.toString();
                        status = vcc.isProved();
                        num_Theorems_chosen++;
                        //continue chooseNewTheorem;
                    }
                    if (substitutionMade == "") {
                        theseResults +=
                                "Emptied queue for "
                                        + cur.m_name
                                        + " with no new results ["
                                        + (System.currentTimeMillis() - time_at_selection)
                                        + "ms]\n\n";
                    }
                }
                else {
                    theseResults +=
                            "Could not find any matches for "
                                    + cur.m_name
                                    + "["
                                    + (System.currentTimeMillis() - time_at_selection)
                                    + "ms]\n\n";
                }
            }
        }
        m_results += theseResults + div;
        return vcc.isProved();

    }



    private String proofFileName() {
        String filePath = tr.getModuleIdentifier().getFile().getPath();
        int temp = filePath.lastIndexOf(".");
        String tempFilePath = filePath.substring(0, temp);
        return tempFilePath + ".proof";
    }

    private void outputProofFile() throws IOException {
        FileWriter w = new FileWriter(new File(proofFileName()));
        w.write("Proofs for " + m_scope.getModuleIdentifier() + " generated " + new Date() + "\n\n");
        w.write(m_results);
        w.write("\n");
        w.flush();
        w.close();
    }
}
