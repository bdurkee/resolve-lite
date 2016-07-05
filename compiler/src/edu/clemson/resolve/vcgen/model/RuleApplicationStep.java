package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.ModelElement;
import edu.clemson.resolve.codegen.model.OutputModelObject;

public class RuleApplicationStep extends OutputModelObject {

    @ModelElement
    public AssertiveBlock step;
    public String description;

    public RuleApplicationStep(AssertiveBlock step, String description) {
        this.step = step;
        this.description = description;
    }
}
