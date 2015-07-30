package edu.clemson.resolve.codegen.model;

import org.rsrg.semantics.programtype.PTVoid;
import org.rsrg.semantics.symbol.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EnhancementImplModule extends AbstractSpecImplModule {
    @ModelElement public List<ConceptDelegateMethod> delegateMethods =
            new ArrayList<>();
    public final String enhancement, concept;

    public EnhancementImplModule(String name, String enhancement,
                                 String concept, ModuleFile file) {
        super(name, file);
        this.enhancement = enhancement;
        this.concept = concept;
    }

    public void addDelegateMethods(List<? extends Symbol> symbols) {
        for (Symbol s : symbols) {
            if ( s instanceof OperationSymbol ) {
                delegateMethods.add(
                        new ConceptDelegateMethod((OperationSymbol)s));
            }
            else if ( s instanceof GenericSymbol ||
                    s instanceof ProgParameterSymbol) {
                delegateMethods.add(
                        new ConceptDelegateMethod("get"+s.getName(), true));
            }
            else if ( s instanceof ProgTypeModelSymbol ) {
                delegateMethods.add(
                        new ConceptDelegateMethod("init"+s.getName(), true));
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
            this(s.getName(), !s.getReturnType().getClass().equals(PTVoid.class),
                    s.getParameters().stream().map(Symbol::getName)
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
