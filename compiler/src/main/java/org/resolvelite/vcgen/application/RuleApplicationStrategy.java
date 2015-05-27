package org.resolvelite.vcgen.application;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveBlock;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

import java.util.List;

public interface RuleApplicationStrategy {

    public AssertiveBlock applyRule(VCAssertiveBlockBuilder block,
                                    List<PExp> statComponents);

    public AssertiveBlock applyRule(VCAssertiveBlockBuilder block, PExp ... e);

    public String getDescription();

}
