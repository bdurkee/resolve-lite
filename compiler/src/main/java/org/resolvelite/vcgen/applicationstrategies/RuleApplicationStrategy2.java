package org.resolvelite.vcgen.applicationstrategies;

import org.resolvelite.proving.absyn.PExp;
import org.resolvelite.vcgen.model.AssertiveCode;
import org.resolvelite.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;

public interface RuleApplicationStrategy2 {

    public AssertiveCode applyRule(VCAssertiveBlockBuilder block, PExp... e);

    public String getDescription();
}
