package edu.clemson.resolve.vcgen.application;

import edu.clemson.resolve.proving.absyn.PApply;
import edu.clemson.resolve.proving.absyn.PExp;
import edu.clemson.resolve.proving.absyn.PSymbol;
import edu.clemson.resolve.vcgen.model.AssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import edu.clemson.resolve.vcgen.model.VCAssume;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class ParsimoniousAssumeApplicationStrategy
        implements
            StatRuleApplicationStrategy<VCAssume> {

    @NotNull @Override public AssertiveBlock applyRule(
            @NotNull VCAssertiveBlockBuilder block,
            @NotNull VCAssume stat) {
        PExp assumeExp = stat.getAssumeExp();
        PExp RP = block.finalConfirm.getConfirmExp();


        return block.snapshot();
    }

    @NotNull @Override public String getDescription() {
        return "parsimonious assume application";
    }
}