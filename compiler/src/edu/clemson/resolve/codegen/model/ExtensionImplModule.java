package edu.clemson.resolve.codegen.model;

import edu.clemson.resolve.semantics.programtype.ProgVoidType;
import edu.clemson.resolve.semantics.symbol.OperationSymbol;
import edu.clemson.resolve.semantics.symbol.Symbol;
import edu.clemson.resolve.semantics.symbol.TypeModelSymbol;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExtensionImplModule extends AbstractSpecImplModule {
    @ModelElement
    public List<ConceptDelegateMethod> delegateMethods =
            new ArrayList<>();
    public final String enhancement;

    public ExtensionImplModule(String name, String enhancement,
                               String concept, ModuleFile file) {
        super(name, concept, file);
        this.enhancement = enhancement;
    }

    public void addDelegateMethods(List<? extends Symbol> symbols) {
        for (Symbol s : symbols) {
            if (s instanceof OperationSymbol) {
                delegateMethods.add(
                        new ConceptDelegateMethod((OperationSymbol) s));
            } else if (s instanceof TypeModelSymbol) {
                delegateMethods.add(
                        new ConceptDelegateMethod("init" + s.getName(), true));
            }
        }
    }

    /**
     * Enhancement implementations are required to implement all methods of the
     * base concept. However, since all enhancement impls receive some
     * instantiation of the base concept through ctor, these methods simply
     * are delegates. For example,
     * <pre>public RType Depth(RType S) {
     *     return conceptual.Depth(S);
     * }</pre>
     */
    public class ConceptDelegateMethod extends OutputModelObject {
        public boolean hasReturn = false;
        public String name;
        public List<String> parameters = new ArrayList<>();

        public ConceptDelegateMethod(OperationSymbol s) {
            this(s.getName(), !s.getReturnType().getClass()
                    .equals(ProgVoidType.class), s.getParameters()
                    .stream().map(Symbol::getName)
                    .collect(Collectors.toList()));
        }

        public ConceptDelegateMethod(String name, boolean hasReturn) {
            this(name, hasReturn, new ArrayList<>());
        }

        public ConceptDelegateMethod(String name, boolean hasReturn,
                                     List<String> parameters) {
            this.name = name;
            this.hasReturn = hasReturn;
            this.parameters.addAll(parameters);
        }
    }
}
