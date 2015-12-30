package edu.clemson.resolve.codegen.model;

import org.rsrg.semantics.symbol.ModuleParameterSymbol;
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

    public static class ExtensionModule extends SpecModule {
        public String concept;

        public ExtensionModule(String name, String concept, ModuleFile file) {
            super(name, file);
            this.concept = concept;
        }
    }

    @Override public void addGettersAndMembersForModuleParameterSyms(
            List<ModuleParameterSymbol> symbols) {
        for (ModuleParameterSymbol p : symbols) {
            funcs.add(buildGetterSignature(p.getName()));
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