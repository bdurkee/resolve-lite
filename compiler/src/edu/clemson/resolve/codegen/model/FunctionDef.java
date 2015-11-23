package edu.clemson.resolve.codegen.model;

import org.rsrg.semantics.symbol.ProgParameterSymbol;

import java.util.ArrayList;
import java.util.List;

public class FunctionDef extends OutputModelObject {
    public boolean hasReturn = false;
    public boolean isStatic = false;
    public String containingModuleName, name;
    @ModelElement public List<ParameterDef> params = new ArrayList<>();

    public FunctionDef(String name) {
        this.name = name;
    }

    public FunctionDef(ProgParameterSymbol specParameter) {
        this("get" + specParameter.getName());
        hasReturn = true;
    }

    //public FunctionDef(Resolve.GenericTypeContext specGeneric) {
    //    this("get" + specGeneric.ID().getText());
    //    hasReturn = true;
    //}
}