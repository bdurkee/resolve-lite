package edu.clemson.resolve.codegen.model;

import org.rsrg.semantics.symbol.GenericSymbol;
import org.rsrg.semantics.symbol.ProgParameterSymbol;
import org.rsrg.semantics.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

public abstract class SpecModule extends Module {
    @ModelElement public List<TypeInterfaceDef> types = new ArrayList<>();
    @ModelElement public List<FunctionDef> funcs = new ArrayList<>();

    public SpecModule(String name, ModuleFile file) {
        super(name, file);
    }

    public static class ConceptModule extends SpecModule {
        public ConceptModule(String name, ModuleFile file) {
            super(name, file);
        }
    }

    public static class EnhancementModule extends SpecModule {
        public String concept;

        public EnhancementModule(String name, String concept, ModuleFile file) {
            super(name, file);
            this.concept = concept;
        }
    }

    @Override public void addGetterMethodsAndVarsForConceptualParamsAndGenerics(
            List<? extends Symbol> symbols) {
        for (Symbol s : symbols) {
            if ( s instanceof ProgParameterSymbol ) {
                funcs.add(buildGetterSignature(s.getName()));
            }
            else if ( s instanceof GenericSymbol ) {
                funcs.add(buildGetterSignature(s.getName()));
            }
        }
    }

    @Override public void addOperationParameterModelObjects(
            FunctionDef wrappedFunction) {}

    private FunctionDef buildGetterSignature(String name) {
        FunctionDef getterFunc = new FunctionDef("get" + name);
        getterFunc.hasReturn = true;
        return getterFunc;
    }
}