package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.semantics.symbol.ModuleParameterSymbol;
import edu.clemson.resolve.semantics.symbol.ProgParameterSymbol;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by daniel on 7/29/15.
 */
public abstract class AbstractSpecImplModule extends Module {
    @ModelElement
    public List<FacilityDef> facilityVars = new ArrayList<>();
    @ModelElement
    public List<OperationParameterDef> opParams =
            new ArrayList<>();
    @ModelElement
    public CtorDef ctor;
    public String concept;

    public AbstractSpecImplModule(String name, String concept, ModuleFile file) {
        super(name, file);
        this.concept = concept;
    }

    public void addGettersAndMembersForModuleParameterSyms(
            List<ModuleParameterSymbol> symbols) {
        for ( ModuleParameterSymbol s : symbols ) {
            if ( s.getWrappedParamSymbol() instanceof ProgParameterSymbol ) {
                funcImpls.add(buildGetterMethod(s.getName()));
                //Note that the variables representing these parameters
                //do not have inits... they get assigned within ctor
                //for this class (which is a separate model object)
                memberVars.add(new VariableDef(s.getName(), null));
            } else if ( s.isModuleTypeParameter() ) {
                funcImpls.add(buildGetterMethod(s.getName()));
                funcImpls.add(buildInitMethod(s.getName()));
                memberVars.add(new VariableDef(s.getName(), null));
            } else if ( s.isModuleOperationParameter() ) {
                funcImpls.add(buildGetterMethod(s.getName()));
                funcImpls.add(buildInitMethod(s.getName()));
                memberVars.add(new VariableDef(s.getName(), null));
            }
        }
    }

    @Override
    public void addOperationParameterModelObjects(
            FunctionDef wrappedFunction) {
        memberVars.add(new VariableDef(wrappedFunction.name, null));
        opParams.add(new OperationParameterDef(wrappedFunction));
    }

    public void addCtor() {
        this.ctor = new CtorDef(this.name, concept, facilityVars, memberVars);
    }

    protected FunctionImpl.InitterFunctionImpl buildInitMethod(String name) {
        return new FunctionImpl.InitterFunctionImpl(name);
    }

    protected FunctionImpl buildGetterMethod(String name) {
        FunctionImpl getterFunc = new FunctionImpl("get" + name);
        //getterFunc.implementsOper = true;
        getterFunc.hasReturn = true;
        getterFunc.stats.add(new ReturnStat(name));
        return getterFunc;
    }
}
