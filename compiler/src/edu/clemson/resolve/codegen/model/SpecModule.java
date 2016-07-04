package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.semantics.symbol.ModuleParameterSymbol;

import java.util.ArrayList;
import java.util.List;

public abstract class SpecModule extends Module {
    @ModelElement
    public List<TypeInterfaceDef> types = new ArrayList<>();
    @ModelElement
    public List<FunctionDef> funcs = new ArrayList<>();

    public SpecModule(String name, ModuleFile file) {
        super(name, file);
    }

    public static class ConceptModule extends SpecModule {
        public ConceptModule(String name, ModuleFile file) {
            super(name, file);
        }
    }

    public static class SpecExtensionModule extends SpecModule {
        public String concept;

        public SpecExtensionModule(String name, String concept, ModuleFile file) {
            super(name, file);
            this.concept = concept;
        }
    }

    @Override
    public void addGettersAndMembersForModuleParameterSyms( List<ModuleParameterSymbol> symbols) {
        for (ModuleParameterSymbol p : symbols) {
            funcs.add(buildGetterSignature(p.getName()));
        }
    }

    @Override
    public void addOperationParameterModelObjects(FunctionDef wrappedFunction) {
    }

    private FunctionDef buildGetterSignature(String name) {
        FunctionDef getterFunc = new FunctionDef("get" + name);
        getterFunc.hasReturn = true;
        return getterFunc;
    }
}