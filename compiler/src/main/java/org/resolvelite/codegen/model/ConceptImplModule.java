package org.resolvelite.codegen.model;

import org.resolvelite.semantics.symbol.GenericSymbol;
import org.resolvelite.semantics.symbol.ProgParameterSymbol;
import org.resolvelite.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public class ConceptImplModule extends Module implements SpecImplModule {
    public String concept;
    @ModelElement public List<FacilityDef> facilityVars = new ArrayList<>();
    @ModelElement public List<OperationParameterDef> opParams =
            new ArrayList<>();
    @ModelElement public CtorDef ctor;

    public ConceptImplModule(String name, String concept, ModuleFile file) {
        super(name, file);
        this.concept = concept;
    }

    @Override public void addCtor() {
        this.ctor = new CtorDef(this.name, facilityVars, memberVars);
    }

    @Override public void
            addGetterMethodsAndVarsForConceptualParamsAndGenerics(
                    List<? extends Symbol> symbols) {
        for (Symbol s : symbols) {
            if ( s instanceof ProgParameterSymbol ) {
                funcImpls.add(buildGetterMethod(s.getName()));
                //Note that the variables representing these parameters
                //do not have inits... they get assigned within ctor
                //for this class (which is a separate model object)
                memberVars.add(new VariableDef(s.getName(), null));
            }
            else if ( s instanceof GenericSymbol ) {
                funcImpls.add(buildGetterMethod(s.getName()));
                funcImpls.add(buildInitMethod(s.getName()));
                memberVars.add(new VariableDef(s.getName(), null));
            }
        }
    }

    @Override public void addOperationParameterModelObjects(
            FunctionDef wrappedFunction) {
        memberVars.add(new VariableDef(wrappedFunction.name, null));
        opParams.add(new OperationParameterDef(wrappedFunction));
    }

    private FunctionImpl buildInitMethod(String name) {
        FunctionImpl initterFunc = new FunctionImpl("init" + name);
        initterFunc.hasReturn = true;
        initterFunc.stats.add(new ReturnStat(name));
        return initterFunc;
    }

    private FunctionImpl buildGetterMethod(String name) {
        FunctionImpl getterFunc = new FunctionImpl("get" + name);
        getterFunc.implementsOper = true;
        getterFunc.hasReturn = true;
        getterFunc.stats.add(new ReturnStat(name));
        return getterFunc;
    }
}