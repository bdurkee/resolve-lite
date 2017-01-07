package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.antlr.v4.runtime.Token;
import org.jetbrains.annotations.NotNull;
import edu.clemson.resolve.semantics.Quantification;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

//This is really just a purely syntactic node to help us know where to print
//quantifiers (at which level), and which vars, etc.
public class PQuantified extends PExp {

    private final Quantification quantificationType;
    private final PExp assertion;
    private final List<PLambda.MathSymbolDeclaration> declaredSymbols = new ArrayList<>();

    public PQuantified(@NotNull PExp assertion,
                       @NotNull Quantification quantificationType,
                       @NotNull List<PLambda.MathSymbolDeclaration> symDecls) {
        this(assertion, quantificationType, symDecls, null, null);
    }

    public PQuantified(@NotNull PExp assertion,
                       @NotNull Quantification quantificationType,
                       @NotNull List<PLambda.MathSymbolDeclaration> symDecls,
                       @Nullable Token vcLocation,
                       @Nullable String vcExplanation) {
        super(assertion.structureHash, assertion.valueHash, assertion.getMathClssftn(), null,
                vcLocation, vcExplanation);
        this.quantificationType = quantificationType;
        this.assertion = assertion;
        this.declaredSymbols.addAll(symDecls);
    }

    @NotNull
    public Quantification getQuantificationType() {
        return quantificationType;
    }

    @NotNull
    public PExp getAssertion() {
        return assertion;
    }

    @NotNull
    public List<PLambda.MathSymbolDeclaration> getDeclaredSymbols() {
        return declaredSymbols;
    }

    @Override
    public PExp withPrimeMarkAdded() {
        return new PQuantified(assertion, quantificationType, declaredSymbols, getVCLocation(), getVCExplanation());
    }

    @Override
    public void accept(PExpListener v) {

    }

    @NotNull
    @Override
    public PExp substitute(@NotNull Map<PExp, PExp> substitutions) {
        return new PQuantified(assertion.substitute(substitutions), quantificationType, declaredSymbols,
                getVCLocation(), getVCExplanation());
    }

    @Override
    public boolean containsName(String name) {
        return assertion.containsName(name);
    }

    @NotNull
    @Override
    public List<? extends PExp> getSubExpressions() {
        List<PExp> result = new ArrayList<>();
        result.add(assertion);
        return result;
    }

    @NotNull
    @Override
    public String getTopLevelOperationName() {
        return "Quantified exp";
    }

    @Override
    protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {
        accumulator.add(this);
    }

    @Override
    public PExp withVCInfo(@Nullable Token location, @Nullable String explanation) {
        return new PQuantified(assertion, quantificationType, declaredSymbols, location, explanation);
    }

    @NotNull
    @Override
    public PExp withIncomingSignsErased() {
        return new PQuantified(assertion.withIncomingSignsErased(), quantificationType, declaredSymbols,
                getVCLocation(), getVCExplanation());
    }

    @NotNull
    @Override
    public PExp withQuantifiersFlipped() {
        return new PQuantified(assertion.withQuantifiersFlipped(), quantificationType.flipped(), declaredSymbols,
                getVCLocation(), getVCExplanation());
    }

    @NotNull
    @Override
    public Set<PSymbol> getIncomingVariablesNoCache() {
        return assertion.getIncomingVariables();
    }

    @NotNull
    @Override
    public Set<PSymbol> getQuantifiedVariablesNoCache() {
        return assertion.getQuantifiedVariables();
    }

    @NotNull
    @Override
    public List<PExp> getFunctionApplicationsNoCache() {
        return assertion.getFunctionApplications();
    }

    @Override
    protected Set<String> getSymbolNamesNoCache(boolean excludeApplications, boolean excludeLiterals) {
        return assertion.getSymbolNames(excludeApplications, excludeLiterals);
    }

    @NotNull
    @Override
    public Set<PSymbol> getFreeVariablesNoCache() {
        return assertion.getQuantifiedVariables();
    }

    @Override
    public String toString() {
        List<String> symNames = Utils.apply(declaredSymbols, PLambda.MathSymbolDeclaration::getName);
        String qType = quantificationType == Quantification.UNIVERSAL ? "∀" : "∃";
        return qType + " " + Utils.join(symNames, ", ") + ":" +
                declaredSymbols.get(0).type + " " + assertion.toString();
    }

    @Override
    public boolean equals(Object o) {
        boolean result = (o instanceof PQuantified);
        if (result) {
            result = assertion.equals(o);
        }
        return result;
    }

}
