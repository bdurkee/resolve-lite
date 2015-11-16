package edu.clemson.resolve.proving.absyn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rsrg.semantics.MTType;
import org.rsrg.semantics.Quantification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PQuantified extends PExp {

    private final Quantification quantificationType;
    private final PExp assertion;

    public PQuantified(@NotNull HashDuple hashes, @NotNull MTType type,
                       @Nullable MTType typeValue,
                       @NotNull PExp assertion,
                       @NotNull Quantification quantificationType) {
        super(hashes, type, typeValue);
        this.quantificationType = quantificationType;
        this.assertion = assertion;
    }

    @Override public void accept(PExpListener v) {

    }

    @NotNull @Override public PExp substitute(
            @NotNull Map<PExp, PExp> substitutions) {
        return null;
    }

    @Override public boolean containsName(String name) {
        return assertion.containsName(name);
    }

    @NotNull @Override public List<? extends PExp> getSubExpressions() {
        List<PExp> result = new ArrayList<>();
        result.add(assertion);
        return result;
    }

    @NotNull @Override protected String getCanonicalName() {
        return "Quantified exp";
    }

    @Override protected void splitIntoConjuncts(@NotNull List<PExp> accumulator) {
        accumulator.add(this);
    }

    @NotNull @Override public PExp withIncomingSignsErased() {
        return assertion.withIncomingSignsErased();
    }

    @NotNull @Override public PExp withQuantifiersFlipped() {
        return null;
    }

    @NotNull @Override public Set<PSymbol> getIncomingVariablesNoCache() {
        return assertion.getIncomingVariables();
    }

    @NotNull @Override public Set<PSymbol> getQuantifiedVariablesNoCache() {
        return null;
    }

    @NotNull @Override public List<PExp> getFunctionApplicationsNoCache() {
        return null;
    }

    @Override protected Set<String> getSymbolNamesNoCache(
            boolean excludeApplications, boolean excludeLiterals) {
        return null;
    }

    @Override public boolean equals(Object o) {
        return false;
    }
}
