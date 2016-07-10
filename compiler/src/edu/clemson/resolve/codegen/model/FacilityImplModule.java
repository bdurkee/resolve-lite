package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.semantics.symbol.ModuleParameterSymbol;

import java.util.ArrayList;
import java.util.List;

public class FacilityImplModule extends Module {
    public String conceptName, definedMain;
    @ModelElement
    public List<FacilityDef> facilities = new ArrayList<>();

    public FacilityImplModule(String name, ModuleFile file) {
        super(name, file);
    }

    public String getDefinedMain() {
        for (FunctionDef f : funcImpls) {
            if (f.name.equalsIgnoreCase("main")) {
                return f.name;
            }
        }
        return null;
    }

    //does nothing for impls. No module params or generics possible...
    @Override
    public void addGettersAndMembersForModuleParameterSyms(
            List<ModuleParameterSymbol> symbols) {
    }

    @Override
    public void addOperationParameterModelObjects(
            FunctionDef wrappedFunction) {
    }
}