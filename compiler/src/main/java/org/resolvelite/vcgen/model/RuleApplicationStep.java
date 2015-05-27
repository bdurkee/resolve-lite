package org.resolvelite.vcgen.model;

import org.resolvelite.codegen.model.ModelElement;
import org.resolvelite.codegen.model.OutputModelObject;

public class RuleApplicationStep extends OutputModelObject {

    public @ModelElement AssertiveBlock step;
    public String description;

    public RuleApplicationStep(AssertiveBlock step, String description) {
        this.step = step;
        this.description = description;
    }
}
