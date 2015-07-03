package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.model.ModelElement;
import edu.clemson.resolve.codegen.model.OutputModelObject;
import edu.clemson.resolve.vcgen.VC;

import java.util.ArrayList;
import java.util.List;

public class VCOutputFile extends OutputModelObject {

    /**
     * All completed {@link AssertiveBlock} objects; where each
     * represents a vc or group of vcs that must be satisfied to verify a parsed
     * program.
     */
    @ModelElement public List<AssertiveBlock> chunks = new ArrayList<>();

    /**
     * The final list of immutable vcs.
     */
    @ModelElement public List<VC> finalVcs = new ArrayList<>();

}
