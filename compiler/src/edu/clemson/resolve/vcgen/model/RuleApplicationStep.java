package edu.clemson.resolve.vcgen.model;

import edu.clemson.resolve.codegen.Model;
import edu.clemson.resolve.codegen.Model.OutputModelObject;
import edu.clemson.resolve.codegen.ModelElement;

public class RuleApplicationStep extends OutputModelObject {

    @ModelElement
    public AssertiveBlock step;
    public String description;

    public RuleApplicationStep(AssertiveBlock step, String description) {
        this.step = step;
        this.description = description;
    }
}
