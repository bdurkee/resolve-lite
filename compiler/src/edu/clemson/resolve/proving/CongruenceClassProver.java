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

    private final PerVCProverModel[] models;
    private final int numTriesBeforeQuitting;
    private final RESOLVECompiler compiler;
    private final AnnotatedModule tr;
    private ProverListener proverListener;

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
        models = new PerVCProverModel[vcs.size()];
        if (compiler.proverListener != null) {
            this.proverListener = compiler.proverListener;
        }
        List<VC> preprocessedVcs = preprocessVCs(vcs);
        //List<VC> preprocessedVcs = new ArrayList<>();

        //VC test = buildTestVC5(m_scope, g, z, n);
        //preprocessedVcs.add(test);
        //m_ccVCs.add(new VerificationConditionCongruenceClosureImpl(g, test, z, n));

        //preprocessedVcs.add(test);
        int i = 0;
        for (VC vc : preprocessedVcs) {
            m_ccVCs.add(new VerificationConditionCongruenceClosureImpl(g, vc, z, n));
            models[i++] = new PerVCProverModel(g, vc.getName(), vc.getAntecedent().splitIntoConjuncts(),
                    vc.getConsequent().splitIntoConjuncts());
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
                    //of a function application (PApply) TODO: bp this(103) when m_theorems is size 7
                    addGoalSearchingTheorem((PApply)assertion, eName);
                    //first arg (.get(1)) of function implies, second arg (.get(2))
                    t =
                            new TheoremCongruenceClosureImpl(g, assertion,
                                    assertion.getSubExpressions().get(1),
                                    assertion.getSubExpressions().get(2),
                                    assertion.getSubExpressions().get(2), true,
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

    private VC buildTestVC1(Scope s, DumbMathClssftnHandler g, MathClssftn z, MathClssftn n) {
        PSymbol pcurrPlace = new PSymbol.PSymbolBuilder("P.Curr_Place").mathClssfctn(z).build();
        PSymbol zero = new PSymbol.PSymbolBuilder("0").mathClssfctn(z).build();

        PSymbol LTE = new PSymbol.PSymbolBuilder("≤")
                .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, z, z))
                .build();
        PSymbol LT = new PSymbol.PSymbolBuilder("<")
                .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, z, z))
                .build();

        //0 <= P.Curr_Place
        PApply zeroLTEpcurrplace = new PApply.PApplyBuilder(LTE)
                .applicationType(g.BOOLEAN)
                .arguments(zero, pcurrPlace)
                .build();

        PSymbol plength = new PSymbol.PSymbolBuilder("P.Length").mathClssfctn(n).build();

        //P.Curr_Place <= P.Length
        PApply pcurrplaceLTEplength = new PApply.PApplyBuilder(LTE)
                .applicationType(g.BOOLEAN)
                .arguments(pcurrPlace, plength)
                .build();

        PSymbol maxlength = new PSymbol.PSymbolBuilder("Max_Length").mathClssfctn(z).build();

        //P.Length < Max_Length
        PApply plengthLTmaxlength = new PApply.PApplyBuilder(LT)
                .applicationType(g.BOOLEAN)
                .arguments(plength, maxlength)
                .build();

        List<PExp> ants = new ArrayList<>();
        ants.add(zeroLTEpcurrplace);
        ants.add(pcurrplaceLTEplength);
        ants.add(plengthLTmaxlength);

        //P.Curr_Place <= Max_Length
        PApply pcurrplaceLTEmaxlength = new PApply.PApplyBuilder(LTE)
                .applicationType(g.BOOLEAN)
                .arguments(pcurrPlace, maxlength)
                .build();

        PExp antecedent = g.formConjuncts(zeroLTEpcurrplace, pcurrplaceLTEplength, plengthLTmaxlength);
        PExp consequent = pcurrplaceLTEmaxlength;
        VC result = new VC(1, antecedent, consequent);
        return result;
    }

    private VC buildTestVC3(Scope s, DumbMathClssftnHandler g, MathClssftn z, MathClssftn n) {
        VC result = null;
        try {
            MathClssftnWrappingSymbol ia = s.queryForOne(new MathSymbolQuery(null, "IA"));
            MathClssftnWrappingSymbol scd = s.queryForOne(new MathSymbolQuery(null, "SCD"));
            MathClssftnWrappingSymbol cen = s.queryForOne(new MathSymbolQuery(null, "Cen"));
            MathClssftnWrappingSymbol sp_loc = s.queryForOne(new MathSymbolQuery(null, "Sp_Loc"));
            MathClssftnWrappingSymbol ss = s.queryForOne(new MathSymbolQuery(null, "SS"));
            MathClssftnWrappingSymbol n2 = s.queryForOne(new MathSymbolQuery(null, "N2"));

            PSymbol pcurrPlace = new PSymbol.PSymbolBuilder("P.Curr_Place").mathClssfctn(n).build();
            PSymbol zero = new PSymbol.PSymbolBuilder("0").mathClssfctn(n).build();

            PSymbol LTE = new PSymbol.PSymbolBuilder("≤")
                    .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, n, n))
                    .build();
            PSymbol LTE_z = new PSymbol.PSymbolBuilder("≤")
                    .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, n, n))
                    .build();
            PSymbol LT = new PSymbol.PSymbolBuilder("<")
                    .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, n, n))
                    .build();

            PSymbol min_int = new PSymbol.PSymbolBuilder("min_int")
                    .mathClssfctn(z)
                    .build();
            PSymbol max_int = new PSymbol.PSymbolBuilder("max_int")
                    .mathClssfctn(z)
                    .build();

            PSymbol k = new PSymbol.PSymbolBuilder("k")
                    .mathClssfctn(n2.getClassification())
                    .build();

            PApply min_intLTEk = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(min_int, k)
                    .build();
            PSymbol maxlength = new PSymbol.PSymbolBuilder("Max_Length").mathClssfctn(n).build();

            PApply k_LTE_max_int = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(k, max_int)
                    .build();

            PApply min_intLTEmax_length = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(min_int, maxlength)
                    .build();

            PApply max_lengthLTEmax_int = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(maxlength, max_int)
                    .build();

            PApply zeroLTEpcurrplace = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(zero, pcurrPlace)
                    .build();

            PSymbol plength = new PSymbol.PSymbolBuilder("P.Length").mathClssfctn(n).build();

            //P.Curr_Place <= P.Length
            PApply pcurrplaceLTEplength = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(pcurrPlace, plength)
                    .build();

            PApply plengthLTmaxlength = new PApply.PApplyBuilder(LT)
                    .applicationType(g.BOOLEAN)
                    .arguments(plength, maxlength)
                    .build();

            //P.Curr_Place <= Max_Length
            PApply pcurrplaceLTEmaxlength = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(pcurrPlace, maxlength)
                    .build();

            //SS
            /*PSymbol ss_exp = new PSymbol(ss.getType(), null, "SS");
*/
            //k
            PSymbol k_exp = new PSymbol.PSymbolBuilder("k")
                    .mathClssfctn(n2.getClassification())
                    .build();

            //Cen
            PSymbol cen_exp = new PSymbol.PSymbolBuilder("Cen")
                    .mathClssfctn(cen.getClassification().enclosingClassification)
                    .build();

            //Cen(k)
            MathClssftn sp_loc_type = new MathFunctionApplicationClssftn(g,
                    new MathFunctionClssftn(g, g.SSET, n2.getClassification()), "Sp_Loc", n2.getClassification());
            PApply cen_app_exp = new PApply.PApplyBuilder(cen_exp)
                    .applicationType(sp_loc_type)
                    .arguments(k_exp)
                    .build();

            //SS
            PSymbol ss_exp = new PSymbol.PSymbolBuilder("SS")
                    .mathClssfctn(ss.getClassification().enclosingClassification)
                    .build();
            //SS(k)
            MathFunctionClssftn ss_app_type = new MathFunctionClssftn(g, sp_loc_type, sp_loc_type);
           //MathClssftn ss_app_type = new MathFunctionApplicationClssftn()
            PApply ss_app_exp = new PApply.PApplyBuilder(ss_exp)
                    .applicationType(ss_app_type)
                    .arguments(k_exp)
                    .build();

            //IA
            PSymbol ia_exp = new PSymbol.PSymbolBuilder("IA")
                    .mathClssfctn(ia.getClassification().enclosingClassification)
                    .build();

            //IA(SS(k), Cen(k), P.Length)
            PApply ia_app_exp = new PApply.PApplyBuilder(ia_exp)
                    .applicationType(sp_loc_type)
                    .arguments(ss_app_exp, cen_app_exp, plength)
                    .build();

            int i;
            i=0;
            //SCD
            PSymbol scd_exp = new PSymbol.PSymbolBuilder("SCD")
                    .mathClssfctn(scd.getClassification().enclosingClassification)
                    .build();
            //SCD(IA(SS, Cen, P.Length))
            PApply scd_app_exp = new PApply.PApplyBuilder(scd_exp)
                    .applicationType(n)
                    .arguments(ia_app_exp)
                    .build();

            PApply scdLTmaxlength = new PApply.PApplyBuilder(LT)
                    .applicationType(g.BOOLEAN)
                    .arguments(scd_app_exp, maxlength)
                    .build();

            PExp antecedent = g.formConjuncts(min_intLTEk, k_LTE_max_int, min_intLTEmax_length, max_lengthLTEmax_int, zeroLTEpcurrplace, pcurrplaceLTEplength, plengthLTmaxlength);
            PExp consequent = scdLTmaxlength;
            //PExp consequent = pcurrplaceLTEmaxlength;
            //PExp eq = new PSymbol.PSymbolBuilder("=").mathClssfctn(g.EQUALITY_FUNCTION).build();
            //PExp consequent = new PApply.PApplyBuilder(eq).applicationType(g.BOOLEAN).arguments(ss_app_exp, cen_exp).build();
            result = new VC(3, antecedent, consequent);

            //givens:
            //0 <= P.Curr_Place
            //P.Curr_Place <= P.Length
            //P.Length < Max_Length

            //goal:
            //SCD(IA(SS, Cen, P.Length)) <= Max_Length
        } catch (SymbolTableException e) {
            e.printStackTrace();
        }
        return result;
    }

    private VC buildTestVC4(Scope s, DumbMathClssftnHandler g, MathClssftn z, MathClssftn n) {
        VC result = null;
        try {
            MathClssftnWrappingSymbol ia = s.queryForOne(new MathSymbolQuery(null, "IA"));
            MathClssftnWrappingSymbol scd = s.queryForOne(new MathSymbolQuery(null, "SCD"));
            MathClssftnWrappingSymbol cen = s.queryForOne(new MathSymbolQuery(null, "Cen"));
            MathClssftnWrappingSymbol sp_loc = s.queryForOne(new MathSymbolQuery(null, "Sp_Loc"));
            MathClssftnWrappingSymbol ss = s.queryForOne(new MathSymbolQuery(null, "SS"));
            MathClssftnWrappingSymbol n2 = s.queryForOne(new MathSymbolQuery(null, "N2"));
            MathClssftnWrappingSymbol is_inside_of = s.queryForOne(new MathSymbolQuery(null, "Is_Inside_of"));

            PSymbol pcurrPlace = new PSymbol.PSymbolBuilder("P.Curr_Place").mathClssfctn(n).build();
            PSymbol zero = new PSymbol.PSymbolBuilder("0").mathClssfctn(n).build();

            PSymbol LTE = new PSymbol.PSymbolBuilder("≤")
                    .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, n, n))
                    .build();
            PSymbol LTE_z = new PSymbol.PSymbolBuilder("≤")
                    .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, n, n))
                    .build();
            PSymbol LT = new PSymbol.PSymbolBuilder("<")
                    .mathClssfctn(new MathFunctionClssftn(g, g.BOOLEAN, n, n))
                    .build();

            PSymbol min_int = new PSymbol.PSymbolBuilder("min_int")
                    .mathClssfctn(z)
                    .build();
            PSymbol max_int = new PSymbol.PSymbolBuilder("max_int")
                    .mathClssfctn(z)
                    .build();

            PSymbol k = new PSymbol.PSymbolBuilder("k")
                    .mathClssfctn(n2.getClassification())
                    .build();

            PApply min_intLTEk = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(min_int, k)
                    .build();
            PSymbol maxlength = new PSymbol.PSymbolBuilder("Max_Length").mathClssfctn(n).build();

            PApply k_LTE_max_int = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(k, max_int)
                    .build();

            PApply min_intLTEmax_length = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(min_int, maxlength)
                    .build();

            PApply max_lengthLTEmax_int = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(maxlength, max_int)
                    .build();

            PApply zeroLTEpcurrplace = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(zero, pcurrPlace)
                    .build();

            PSymbol plength = new PSymbol.PSymbolBuilder("P.Length").mathClssfctn(n).build();

            //P.Curr_Place <= P.Length
            PApply pcurrplaceLTEplength = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(pcurrPlace, plength)
                    .build();

            //P.Length <= Max_Length
            PApply plengthLTEmaxlength = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(plength, maxlength)
                    .build();

            //P.Curr_Place <= Max_Length
            PApply pcurrplaceLTEmaxlength = new PApply.PApplyBuilder(LTE)
                    .applicationType(g.BOOLEAN)
                    .arguments(pcurrPlace, maxlength)
                    .build();

            //SS
            /*PSymbol ss_exp = new PSymbol(ss.getType(), null, "SS");
*/
            //k
            PSymbol k_exp = new PSymbol.PSymbolBuilder("k")
                    .mathClssfctn(n2.getClassification())
                    .build();

            //Cen
            PSymbol cen_exp = new PSymbol.PSymbolBuilder("Cen")
                    .mathClssfctn(cen.getClassification().enclosingClassification)
                    .build();

            //Cen(k)
            MathClssftn sp_loc_type = new MathFunctionApplicationClssftn(g,
                    new MathFunctionClssftn(g, g.SSET, n2.getClassification()), "Sp_Loc", n2.getClassification());
            PApply cen_app_exp = new PApply.PApplyBuilder(cen_exp)
                    .applicationType(sp_loc_type)
                    .arguments(k_exp)
                    .build();

            //SS
            PSymbol ss_exp = new PSymbol.PSymbolBuilder("SS")
                    .mathClssfctn(ss.getClassification().enclosingClassification)
                    .build();
            //SS(k)
            MathFunctionClssftn ss_app_type = new MathFunctionClssftn(g, sp_loc_type, sp_loc_type);
            //MathClssftn ss_app_type = new MathFunctionApplicationClssftn()
            PApply ss_app_exp = new PApply.PApplyBuilder(ss_exp)
                    .applicationType(ss_app_type)
                    .arguments(k_exp)
                    .build();

            //IA
            PSymbol ia_exp = new PSymbol.PSymbolBuilder("IA")
                    .mathClssfctn(ia.getClassification().enclosingClassification)
                    .build();

            //IA(SS(k), Cen(k), P.Length)
            PApply ia_app_exp = new PApply.PApplyBuilder(ia_exp)
                    .applicationType(sp_loc_type)
                    .arguments(ss_app_exp, cen_app_exp, plength)
                    .build();

            int i;
            i=0;
            //SCD
            PSymbol scd_exp = new PSymbol.PSymbolBuilder("SCD")
                    .mathClssfctn(scd.getClassification().enclosingClassification)
                    .build();
            //SCD(IA(SS, Cen, P.Length))
            PApply scd_app_exp = new PApply.PApplyBuilder(scd_exp)
                    .applicationType(n)
                    .arguments(ia_app_exp)
                    .build();

            PApply scdLTmaxlength = new PApply.PApplyBuilder(LT)
                    .applicationType(g.BOOLEAN)
                    .arguments(scd_app_exp, maxlength)
                    .build();

            //IA
            PSymbol ia_exp0 = new PSymbol.PSymbolBuilder("IA")
                    .mathClssfctn(ia.getClassification().enclosingClassification)
                    .build();

            //IA(SS(k), Cen(k), P.Curr_Place)
            PApply ia_app_exp0 = new PApply.PApplyBuilder(ia_exp)
                    .applicationType(sp_loc_type)
                    .arguments(ss_app_exp, cen_app_exp, pcurrPlace)
                    .build();
            PApply ia_app_exp1 = ia_app_exp;
            PSymbol inside_of_exp = new PSymbol.PSymbolBuilder("Is_Inside_of")
                    .mathClssfctn(is_inside_of.getClassification().enclosingClassification)
                    .build();

            PApply ia_app_exp0_IsInsideOf_ia_app_exp1 = new PApply.PApplyBuilder(inside_of_exp)
                    .applicationType(g.BOOLEAN)
                    .arguments(ia_app_exp0, ia_app_exp1)
                    .build();

            PExp antecedent = g.formConjuncts(min_intLTEk, k_LTE_max_int, min_intLTEmax_length, max_lengthLTEmax_int, zeroLTEpcurrplace, pcurrplaceLTEplength, plengthLTEmaxlength );
            PExp consequent = ia_app_exp0_IsInsideOf_ia_app_exp1;
            //PExp consequent = pcurrplaceLTEmaxlength;
            //PExp eq = new PSymbol.PSymbolBuilder("=").mathClssfctn(g.EQUALITY_FUNCTION).build();
            //PExp consequent = new PApply.PApplyBuilder(eq).applicationType(g.BOOLEAN).arguments(ss_app_exp, cen_exp).build();
            result = new VC(4, antecedent, consequent);

            //givens:
            //0 <= P.Curr_Place
            //P.Curr_Place <= P.Length
            //P.Length < Max_Length

            //goal:
            //SCD(IA(SS, Cen, P.Length)) <= Max_Length
        } catch (SymbolTableException e) {
            e.printStackTrace();
        }
        return result;
    }

    public VC buildTestVC5(Scope s, DumbMathClssftnHandler g, MathClssftn z, MathClssftn n) {
        VC result = null;
        try {
            MathClssftnWrappingSymbol ia = s.queryForOne(new MathSymbolQuery(null, "IA"));
            MathClssftnWrappingSymbol scd = s.queryForOne(new MathSymbolQuery(null, "SCD"));
            MathClssftnWrappingSymbol cen = s.queryForOne(new MathSymbolQuery(null, "Cen"));
            MathClssftnWrappingSymbol sp_loc = s.queryForOne(new MathSymbolQuery(null, "Sp_Loc"));
            MathClssftnWrappingSymbol ss = s.queryForOne(new MathSymbolQuery(null, "SS"));
            MathClssftnWrappingSymbol n2 = s.queryForOne(new MathSymbolQuery(null, "N2"));
            MathClssftnWrappingSymbol is_inside_of = s.queryForOne(new MathSymbolQuery(null, "Is_Inside_of"));

            MathClssftn sp_loc_type = new MathFunctionApplicationClssftn(g,
                    new MathFunctionClssftn(g, g.SSET, n2.getClassification()), "Sp_Loc", n2.getClassification());

            PSymbol pcurrLoc = new PSymbol.PSymbolBuilder("P.Curr_Loc").mathClssfctn(sp_loc_type).build();
            PSymbol ptrmnlLoc = new PSymbol.PSymbolBuilder("P.Trmnl_Loc").mathClssfctn(sp_loc_type).build();

            //=
            PSymbol EQ = new PSymbol.PSymbolBuilder("=").mathClssfctn(g.EQUALITY_FUNCTION).build();

            //SCD
            PSymbol scd_exp = new PSymbol.PSymbolBuilder("SCD")
                    .mathClssfctn(scd.getClassification().enclosingClassification)
                    .build();

            //SCD(P.Curr_Loc)
            PApply scd_app_exp1 = new PApply.PApplyBuilder(scd_exp)
                    .applicationType(n)
                    .arguments(pcurrLoc)
                    .build();

            //SCD(P.Trmnl_Loc)
            PApply scd_app_exp2 = new PApply.PApplyBuilder(scd_exp)
                    .applicationType(n)
                    .arguments(ptrmnlLoc)
                    .build();

            //SCD(P.Curr_Loc) = SCD(P.Trmnl_Loc)
            PApply scd_equals_app = new PApply.PApplyBuilder(EQ)
                    .applicationType(g.BOOLEAN)
                    .arguments(scd_app_exp1, scd_app_exp2)
                    .build();

            //P.Curr_Loc = P.Trmnl_Loc
            PApply loc_equals_app = new PApply.PApplyBuilder(EQ)
                    .applicationType(g.BOOLEAN)
                    .arguments(pcurrLoc, ptrmnlLoc)
                    .build();

            //(SCD(P.Curr_Loc) = SCD(P.Trmnl_Loc)) = (P.Curr_Loc = P.Trmnl_Loc)
            PApply top_lvl_equals = new PApply.PApplyBuilder(EQ)
                    .applicationType(g.BOOLEAN)
                    .arguments(scd_equals_app, loc_equals_app)
                    .build();

            PExp antecedent = g.getTrueExp();
            PExp consequent = top_lvl_equals;
            result = new VC(5, antecedent, consequent);
        } catch (SymbolTableException e) {
            e.printStackTrace();
        }
        return result;
    }

    private List<VC> preprocessVCs(List<VC> vcs) {
        List<VC> result = new ArrayList<>();
        for (VC vc : vcs) {
            PExp newAntecedent = vc.getAntecedent();
            PExp newConsequent = vc.getConsequent();
            newAntecedent = Utilities.flattenPSelectors(newAntecedent);
            newConsequent = Utilities.flattenPSelectors(newConsequent);

            result.add(new VC(vc.getNumber(), newAntecedent, newConsequent));
            // make every PExp a PSymbol
            //vc.convertAllToPsymbols(m_typeGraph);
            //result.add()
        }
        //result.addAll(vcs); //TODO: Not doing the conversions now. (I don't use lambdas right now, etc)
        return result;
    }

    //TODO: Need to use one with import recursive...
    @Nullable
    private static MathClssftnWrappingSymbol getMathSymbol(@NotNull Scope s, @NotNull String name)
            throws SymbolTableException {
        Symbol result = s.queryForOne(new NameQuery(null, name,
                MathSymbolTable.ImportStrategy.IMPORT_RECURSIVE,
                MathSymbolTable.FacilityStrategy.FACILITY_GENERIC, false));
        if (!(result instanceof MathClssftnWrappingSymbol)) {
            throw new NoSuchSymbolException();
        }
        return (MathClssftnWrappingSymbol)result;
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
                .arguments(theorem.getSubExpressions().get(1), goal)
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
                if (proverListener != null) {
                    proverListener.vcResult(false, models[i], new Metrics(0, 0));
                }
                summary += vcc.m_name + " skipped\n";
                ++i;
                continue;
            }

            VerificationConditionCongruenceClosureImpl.STATUS proved = null;
            if (isCancelled()) {
                whyQuit += "Cancelled";
                proved = VerificationConditionCongruenceClosureImpl.STATUS.CANCELLED;
                numUnproved++;
            }
            else {
                proved = prove(vcc);
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
            }
            long endTime = System.nanoTime();
            long delayNS = endTime - startTime;
            long delayMS = TimeUnit.MILLISECONDS.convert(delayNS, TimeUnit.NANOSECONDS);
            summary += vcc.m_name + whyQuit + " time: " + delayMS + " ms\n";
            if (proverListener != null) {
                this.proverListener
                        .vcResult(
                                (proved == (VerificationConditionCongruenceClosureImpl.STATUS.PROVED) ||
                                        (proved == VerificationConditionCongruenceClosureImpl.STATUS.FALSE_ASSUMPTION)),
                                models[i], new Metrics(delayMS, timeout));
            }
            i++;

        }
        totalTime = System.currentTimeMillis() - totalTime;
        summary += "Elapsed time from construction: " + totalTime + " ms" + "\n";
        String div = divLine("Summary");
        summary = div + summary + div;
        System.out.println(m_results + summary);
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
                && System.currentTimeMillis() <= endTime && !isCancelled()) {
            long time_at_theorem_pq_creation = System.currentTimeMillis();
            // ++++++ Creates new PQ with all the theorems

            //NOTE: DAN, DONE ON A PER VC BASIS. A thm prioritizer per vc.
            TheoremPrioritizer rankedTheorems =
                    new TheoremPrioritizer(theoremsForThisVC,
                            theoremAppliedCount, vcc,
                            m_nonQuantifiedTheoremSymbols, m_smallEndEquations);
            int max_Theorems_to_choose = 1;
            int num_Theorems_chosen = 0;
            while (!isCancelled() && !rankedTheorems.m_pQueue.isEmpty()
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

    public boolean isCancelled() {
        return proverListener != null && proverListener.isCancelled();
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
