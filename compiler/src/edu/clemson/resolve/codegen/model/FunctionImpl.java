package edu.clemson.resolve.codegen.model;

import java.util.ArrayList;
import java.util.List;

public class FunctionImpl extends FunctionDef {
    public boolean hasReturn = false;
    public boolean isStatic = false;
    public boolean implementsOper = false;
    @ModelElement public List<VariableDef> vars = new ArrayList<>();
    @ModelElement public List<Stat> stats = new ArrayList<>();

    public FunctionImpl(String name) {
        super(name);
    }
}