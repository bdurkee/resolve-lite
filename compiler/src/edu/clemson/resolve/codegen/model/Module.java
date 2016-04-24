package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.semantics.symbol.ModuleParameterSymbol;

import java.util.ArrayList;
import java.util.List;

public abstract class Module extends OutputModelObject {
    public String name;
    public ModuleFile file;
    @ModelElement
    public List<FunctionImpl> funcImpls = new ArrayList<>();
    @ModelElement
    public List<MemberClassDef> repClasses = new ArrayList<>();
    @ModelElement
    public List<VariableDef> memberVars = new ArrayList<>();

    public Module(String name, ModuleFile file) {
        this.name = name;
        this.file = file;//who contains us?
    }

    /**
     * Like the name suggests, adds getters and member variabes for the formal
     * parameters to a concept (or enhancement).
     */
    public abstract void addGettersAndMembersForModuleParameterSyms(
            List<ModuleParameterSymbol> symbols);

    /**
     * For implementations that take an operation as a parameter, this method
     * adds both an RType member variable pointing to the interface wrapping
     * the 'operation' as well as the interior interfaces wrapping calls to
     * the operation.
     *
     * @param wrappedFunction
     */
    public abstract void addOperationParameterModelObjects(
            FunctionDef wrappedFunction);
}