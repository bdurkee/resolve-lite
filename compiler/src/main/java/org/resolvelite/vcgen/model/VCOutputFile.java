package org.resolvelite.vcgen.model;

import org.resolvelite.codegen.model.ModelElement;
import org.resolvelite.codegen.model.OutputModelObject;

import java.util.ArrayList;
import java.util.List;

public class VCOutputFile extends OutputModelObject {

    /**
     * A list consisting of {@link AssertiveCode} objects, where each
     * represents a vc or group of vcs that must be satisfied to verify a parsed
     * program.
     */
    @ModelElement public List<AssertiveCode> chunks = new ArrayList<>();

}
