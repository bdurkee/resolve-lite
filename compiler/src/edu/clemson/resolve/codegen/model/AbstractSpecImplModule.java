package edu.clemson.resolve.codegen.model;

import org.rsrg.semantics.symbol.GenericSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by daniel on 7/29/15.
 */
public abstract class AbstractSpecImplModule extends Module {
    @ModelElement public List<FacilityDef> facilityVars = new ArrayList<>();
    @ModelElement public List<OperationParameterDef> opParams =
            new ArrayList<>();
    @ModelElement public CtorDef ctor;

    public AbstractSpecImplModule(String name, ModuleFile file) {
        super(name, file);
    }

    @Override public void addGetterMethodsAndVarsForConceptualParamsAndGenerics(
            List<? extends Symbol> symbols) {
        for (Symbol s : symbols) {
            if ( s instanceof ProgParameterSymbol) {
                funcImpls.add(buildGetterMethod(s.getName()));
                //Note that the variables representing these parameters
                //do not have inits... they get assigned within ctor
                //for this class (which is a separate model object)
                memberVars.add(new VariableDef(s.getName(), null));
            }
            else if ( s instanceof GenericSymbol) {
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

    public void addCtor() {
        this.ctor = new CtorDef(this.name, facilityVars, memberVars);
    }

    protected FunctionImpl buildInitMethod(String name) {
        FunctionImpl initterFunc = new FunctionImpl("init" + name);
        initterFunc.hasReturn = true;
        initterFunc.stats.add(new ReturnStat(name));
        return initterFunc;
    }

    protected FunctionImpl buildGetterMethod(String name) {
        FunctionImpl getterFunc = new FunctionImpl("get" + name);
        getterFunc.implementsOper = true;
        getterFunc.hasReturn = true;
        getterFunc.stats.add(new ReturnStat(name));
        return getterFunc;
    }
}
