package org.resolvelite.vcgen.model;

import org.resolvelite.codegen.model.ModelElement;
import org.resolvelite.codegen.model.OutputModelObject;
import org.resolvelite.vcgen.VC;

import java.util.ArrayList;
import java.util.List;

public class VCOutputFile extends OutputModelObject {

    /**
     * All completed {@link AssertiveCode} objects; where each
     * represents a vc or group of vcs that must be satisfied to verify a parsed
     * program.
     */
    @ModelElement public List<AssertiveCode> chunks = new ArrayList<>();

    /**
     * The final list of immutable vcs.
     */
    @ModelElement public List<VC> finalVcs = new ArrayList<>();

}
