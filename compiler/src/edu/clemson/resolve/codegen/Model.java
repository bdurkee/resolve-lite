package edu.clemson.resolve.codegen;

import edu.clemson.resolve.RESOLVECompiler;
import edu.clemson.resolve.compiler.AnnotatedModule;
import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.semantics.programtype.ProgNamedType;
import edu.clemson.resolve.semantics.programtype.ProgType;
import edu.clemson.resolve.semantics.programtype.ProgVoidType;
import edu.clemson.resolve.semantics.symbol.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Model {

    public static abstract class OutputModelObject {
    }

    //----------------------------------------------------------------------------
    // MODULE FILE
    //----------------------------------------------------------------------------

    public static class ModuleFile extends OutputModelObject {
        public String RESOLVEVersion;
        public String resolveFileName;

        @ModelElement
        public Module module;
        public String genPackage; // should be retreived from target modules ModuleIdentifier
        public List<String> imports = new ArrayList<>();

        public ModuleFile(AnnotatedModule e, String resolveFileName, String pkg, List<String> imports) {
            this.resolveFileName = resolveFileName;
            this.RESOLVEVersion = RESOLVECompiler.VERSION;
            this.genPackage = pkg;
            this.imports.addAll(imports);
        }
    }

    //----------------------------------------------------------------------------
    // MODULES
    //----------------------------------------------------------------------------

    public static abstract class Module extends OutputModelObject {
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
         * Like the name suggests, adds getters and member variabes for the formal parameters to a concept
         * (or enhancement).
         */
        public void addGettersAndMembersForModuleParameterSyms(List<ModuleParameterSymbol> symbols) {
        }

        /**
         * For implementations that take an operation as a parameter, this method adds both an RType member variable
         * pointing to the interface wrapping the 'operation' as well as the interior interfaces wrapping calls to the
         * operation.
         *
         * @param wrappedFunction
         */
        public void addOperationParameterModelObjects(FunctionDef wrappedFunction) {
        }

        /** We gen a dummy class for precis to make java happy. Could be filtered too, but this is easier. */
        public static class PrecisModule extends Module {
            public PrecisModule(String name, ModuleFile file) {
                super(name, file);
            }
        }
    }

    public static abstract class AbstractSpecModule extends Module {
        @ModelElement
        public List<TypeInterfaceDef> types = new ArrayList<>();
        @ModelElement
        public List<FunctionDef> funcs = new ArrayList<>();

        public AbstractSpecModule(String name, ModuleFile file) {
            super(name, file);
        }

        @Override
        public void addGettersAndMembersForModuleParameterSyms( List<ModuleParameterSymbol> symbols) {
            for (ModuleParameterSymbol p : symbols) {
                funcs.add(buildGetterSignature(p.getName()));
            }
        }

        private FunctionDef buildGetterSignature(String name) {
            FunctionDef getterFunc = new FunctionDef("get" + name);
            getterFunc.hasReturn = true;
            return getterFunc;
        }
    }

    /**
     * Created by daniel on 7/29/15.
     */
    public static abstract class AbstractSpecImplModule extends Module {
        @ModelElement
        public List<FacilityDef> facilityVars = new ArrayList<>();
        @ModelElement
        public List<OperationParameterDef> opParams = new ArrayList<>();
        @ModelElement
        public CtorDef ctor;
        public String concept;

        public AbstractSpecImplModule(String name, String concept, ModuleFile file) {
            super(name, file);
            this.concept = concept;
        }

        public void addGettersAndMembersForModuleParameterSyms(List<ModuleParameterSymbol> symbols) {
            for (ModuleParameterSymbol s : symbols) {
                if (s.getWrappedParamSymbol() instanceof ProgParameterSymbol) {
                    funcImpls.add(buildGetterMethod(s.getName()));
                    //Note that the variables representing these parameters
                    //do not have inits... they get assigned within ctor
                    //for this class (which is a separate model object)
                    memberVars.add(new VariableDef(s.getName(), null));
                }
                else if (s.isModuleTypeParameter()) {
                    funcImpls.add(buildGetterMethod(s.getName()));
                    funcImpls.add(buildInitMethod(s.getName()));
                    memberVars.add(new VariableDef(s.getName(), null));
                }
                else if (s.isModuleOperationParameter()) {
                    funcImpls.add(buildGetterMethod(s.getName()));
                    funcImpls.add(buildInitMethod(s.getName()));
                    memberVars.add(new VariableDef(s.getName(), null));
                }
            }
        }

        @Override
        public void addOperationParameterModelObjects(FunctionDef wrappedFunction) {
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

    public static class ConceptImplModule extends AbstractSpecImplModule {
        public ConceptImplModule(String name, String concept, ModuleFile file) {
            super(name, concept, file);
        }
    }

    public static class FacilityImplModule extends Module {
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
    }

    public static class ExtensionImplModule extends AbstractSpecImplModule {
        @ModelElement
        public List<ConceptDelegateMethodDef> delegateMethods = new ArrayList<>();
        public final String enhancement;

        public ExtensionImplModule(String name, String concept, String enhancement, ModuleFile file) {
            super(name, concept, file);
            this.enhancement = enhancement;
        }

        public void addDelegateMethods(List<? extends Symbol> symbols) {
            for (Symbol s : symbols) {
                if (s instanceof OperationSymbol) {
                    delegateMethods.add(new ConceptDelegateMethodDef((OperationSymbol) s));
                }
                else if (s instanceof TypeModelSymbol) {
                    delegateMethods.add(new ConceptDelegateMethodDef("init" + s.getName(), true));
                }
            }
        }
    }

    public static class SpecExtensionModule extends AbstractSpecModule {
        public String concept;

        public SpecExtensionModule(String name, String concept, ModuleFile file) {
            super(name, file);
            this.concept = concept;
        }
    }

    public static class ConceptModule extends AbstractSpecModule {
        public ConceptModule(String name, ModuleFile file) {
            super(name, file);
        }
    }

    //----------------------------------------------------------------------------
    // QUALIFIERS
    //----------------------------------------------------------------------------

    public static abstract class AbstractQualifier extends OutputModelObject {
    }

    public static class FacilityQualifier extends AbstractQualifier {
        public String fullyQualifiedSymbolSpecName, facilityName;

        public FacilityQualifier(String fullyQualifiedSymbolSpecName, String facilityName) {
            this.facilityName = facilityName;
            this.fullyQualifiedSymbolSpecName = fullyQualifiedSymbolSpecName;
        }
    }

    /**
     * I realize "NormalQualifier" isn't a very descriptive name. Just know that it's intended to mean qualifiers
     * without weird casts going on. Think: {@code "ResolveBase.", "this.", "Test_Fac."} etc, etc.
     */
    public static class NormalQualifier extends AbstractQualifier {
        public String name;
        public NormalQualifier(String qualifierName) {
            this.name = qualifierName;
        }
    }

    //----------------------------------------------------------------------------
    // EXPRS
    //----------------------------------------------------------------------------

    public static abstract class Expr extends OutputModelObject {
    }

    public static class AccessRef extends Expr {
        @ModelElement
        public final Expr left, right;

        public AccessRef(Expr l, Expr r) {
            this.left = l;
            this.right = r;
        }

        public static class LeafAccessRefLeft extends Expr {
            @ModelElement
            public Expr name;
            public String type;

            public LeafAccessRefLeft(String type, Expr name) {
                this.name = name;
                this.type = type;
            }
        }

        public static class LeafAccessRefRight extends Expr {
            @ModelElement
            public Expr name;

            public LeafAccessRefRight(Expr name) {
                this.name = name;
            }
        }
    }

    /**
     * Represents an anonymous class that wraps the invocation of an operation
     * that we wish to pass into another module (via a facility, etc).
     */
    public static class AnonOpParameterClassInstance extends Expr {
        public String name;
        public boolean hasReturn = false;
        public List<ProgParameterSymbol> params = new ArrayList<>();
        @ModelElement
        public AbstractQualifier q;

        public AnonOpParameterClassInstance(AbstractQualifier wrappedFunctionQualifier,
                                            OperationSymbol f) {
            this.name = f.getName();
            this.q = wrappedFunctionQualifier;
            this.hasReturn = !(f.getReturnType() instanceof ProgVoidType);
            this.params = f.getParameters();
        }
    }

    public static class MemberRef extends Expr {
        public String name;

        @ModelElement
        public Expr child;
        public String typeName, typeQualifier;
        public boolean isBaseRef = false;
        public boolean isLastRef = false;

        public MemberRef(String name, String typeName, String typeQualifier) {
            this.name = name;
            this.typeName = typeName;
            this.typeQualifier = typeQualifier;
            this.isBaseRef = isBaseRef;
        }

        public MemberRef(String name, ProgType t) {
            this(name, ((ProgNamedType) t).getName(), ((ProgNamedType) t)
                    .getModuleIdentifier().getNameToken().getText());
        }
    }

    /**
     * This class is different (and neccessary) even though we already have an {@link CallStat} class. This is intended
     * to represent calls appearing within the context of an arbitrary expression.
     */
    public static class MethodCall extends Expr {
        public String name;
        @ModelElement
        public List<Expr> args = new ArrayList<>();
        @ModelElement
        public AbstractQualifier q;

        public MethodCall(AbstractQualifier qualifier, String name, Expr... args) {
            this(qualifier, name, Arrays.asList(args));
        }

        public MethodCall(AbstractQualifier qualifier, String name, List<Expr> args) {
            this.name = name;
            this.q = qualifier;
            this.args.addAll(args);
        }

        /**
         * Two special constructors used to create create getter calls for variables
         * referencing things like module level generics and formal params
         */
        public MethodCall(VarNameRef nameRef) {
            this(nameRef.q, "get" + nameRef.name, Collections.emptyList());
        }

        public MethodCall(TypeInit genericTypeInit) {
            this(genericTypeInit.q, "get" + genericTypeInit.typeName, Collections.emptyList());
        }

        //i.e.: ((OperationParameter) Read_Element).op(Next);
        public static class OperationParameterMethodCall extends Expr {
            public String name;
            @ModelElement
            public List<Expr> args = new ArrayList<>();

            public OperationParameterMethodCall(String name, List<Expr> args) {
                this.name = name;
                this.args.addAll(args);
            }
        }
    }

    public static class TypeInit extends Expr {
        @ModelElement
        public AbstractQualifier q;
        public String typeName, initialValue;

        public TypeInit(AbstractQualifier q, String typeName, String initialValue) {
            this.q = q;
            this.typeName = typeName;
            this.initialValue = initialValue;
        }
    }

    public static class VarNameRef extends Expr {
        public String name;
        @ModelElement
        public AbstractQualifier q;

        public VarNameRef(AbstractQualifier qualifier, String name) {
            this.name = name;
            this.q = qualifier;
        }
    }

    //----------------------------------------------------------------------------
    // DEFS
    //----------------------------------------------------------------------------

    public static class ParameterDef extends OutputModelObject {
        public String name;
        public ParameterDef(String name) {
            this.name = name;
        }
    }

    public static class TypeInterfaceDef extends OutputModelObject {
        public String name;
        public TypeInterfaceDef(String name) {
            this.name = name;
        }
    }

    public static class VariableDef extends OutputModelObject {
        @ModelElement
        public Expr init; //in practice, usually MethodCall and FacilityDefinedTypeInit
        public String name;

        public VariableDef(String name, Expr init) {
            this.name = name;
            this.init = init;
        }
    }

    public static class OperationParameterDef extends OutputModelObject {
        @ModelElement
        public FunctionDef func;
        public String name;

        public OperationParameterDef(FunctionDef f) {
            this.func = f;
            this.name = f.name;
        }
    }

    public static class MemberClassDef extends OutputModelObject {
        public boolean isStatic = false;
        public String name, referredToByExemplar;

        @ModelElement
        public List<VariableDef> fields = new ArrayList<>();

        /**
         * Holds all variable defs and stats describing some type reprs process of initialization; as defined within
         * the @{code typeImplInit} rule.
         */
        @ModelElement
        public List<VariableDef> initVars = new ArrayList<>();
        @ModelElement
        public List<Stat> initStats = new ArrayList<>();

        public MemberClassDef(String name) {
            this.name = name;
        }

    }

    public static class FunctionDef extends OutputModelObject {
        public boolean hasReturn = false;
        public boolean isStatic = false;
        public String containingModuleName, name;
        @ModelElement
        public List<ParameterDef> params = new ArrayList<>();

        public FunctionDef(String name) {
            this.name = name;
        }

        public FunctionDef(ProgParameterSymbol specParameter) {
            this("get" + specParameter.getName());
            hasReturn = true;
        }
    }

    public static class FacilityDef extends OutputModelObject {
        public boolean isStatic = false;
        public String name, concept;
        @ModelElement
        public DecoratedFacilityInstantiation root;

        public FacilityDef(String name, String concept) {
            this.name = name;
            this.concept = concept;
        }

        public void addGettersForGenericsAndNamedVariableArguments(
                List<? extends Symbol> symbols) {
        }
    }

    public static class DecoratedFacilityInstantiation extends OutputModelObject {
        public boolean isProxied;
        public String specName, specRealizName;
        @ModelElement
        public List<Expr> args = new ArrayList<>();
        @ModelElement
        public DecoratedFacilityInstantiation child;

        public DecoratedFacilityInstantiation(String specName, String specRealizName) {
            this.specName = specName;
            this.specRealizName = specRealizName;
        }
    }


    public static class CtorDef extends OutputModelObject {

        public String name, delegateInterface;
        public List<String> members = new ArrayList<>();
        @ModelElement
        public List<FacilityDef> facMems = new ArrayList<>();

        public CtorDef(String name, String delegateInterface,
                       List<FacilityDef> facilityVars,
                       List<VariableDef> memberVars) {
            this.name = name;
            this.delegateInterface = delegateInterface;
            this.members.addAll(memberVars.stream().map(v -> v.name).collect(Collectors.toList()));
            this.facMems.addAll(facilityVars);
        }
    }

    /**
     * Enhancement implementations are required to implement all methods of the base concept. However, since all
     * enhancement impls receive some instantiation of the base concept through ctor, these methods simply are
     * delegates. For example,
     * <pre>public RType Depth(RType S) {
     *     return conceptual.Depth(S);
     * }</pre>
     */
    public static class ConceptDelegateMethodDef extends OutputModelObject {
        public boolean hasReturn = false;
        public String name;
        public List<String> parameters = new ArrayList<>();

        public ConceptDelegateMethodDef(OperationSymbol s) {
            this(s.getName(), !s.getReturnType().getClass().equals(ProgVoidType.class),
                    Utils.apply( s.getParameters(), Symbol::getName));
        }

        public ConceptDelegateMethodDef(String name, boolean hasReturn) {
            this(name, hasReturn, new ArrayList<>());
        }

        public ConceptDelegateMethodDef(String name, boolean hasReturn,
                                        List<String> parameters) {
            this.name = name;
            this.hasReturn = hasReturn;
            this.parameters.addAll(parameters);
        }
    }

    public static class FunctionImpl extends FunctionDef {
        public boolean hasReturn = false;
        public boolean isStatic = false;
        public boolean implementsOper = false;
        @ModelElement
        public List<VariableDef> vars = new ArrayList<>();
        @ModelElement
        public List<Stat> stats = new ArrayList<>();

        public FunctionImpl(String name) {
            super(name);
        }

        public static class InitterFunctionImpl extends FunctionImpl {
            public InitterFunctionImpl(String name) {
                super(name);
            }
        }
    }

    //----------------------------------------------------------------------------
    // STATS
    //----------------------------------------------------------------------------

    public static abstract class Stat extends OutputModelObject {
    }

    public static class CallStat extends Stat {
        @ModelElement
        public Expr methodParamExp;

        public CallStat(AbstractQualifier qualifier, String name, Expr... args) {
            this(qualifier, name, Arrays.asList(args));
        }

        public CallStat(Expr expr) {
            if (expr instanceof MethodCall.OperationParameterMethodCall || expr instanceof MethodCall) {
                this.methodParamExp = expr;
            }
            else {
                throw new IllegalArgumentException("expr doesn't describe a call");
            }
        }

        public CallStat(MethodCall exprCall) {
            this(exprCall.q, exprCall.name, exprCall.args);
        }

        public CallStat(AbstractQualifier qualifier, String name, List<Expr> args) {
            this.methodParamExp = new MethodCall(qualifier, name, args);
        }

        public CallStat(MethodCall.OperationParameterMethodCall exprCall) {
            this.methodParamExp = exprCall;
        }
    }

    public static class IfStat extends Stat {
        @ModelElement
        public List<Stat> ifStats = new ArrayList<>();
        @ModelElement
        public List<Stat> elseStats = new ArrayList<>();
        @ModelElement
        public Expr cond;

        public IfStat(Expr cond) {
            this.cond = cond;
        }
    }

    public static class WhileStat extends Stat {
        @ModelElement
        public Expr cond;
        @ModelElement
        public List<Stat> stats = new ArrayList<>();

        public WhileStat(Expr cond) {
            this.cond = cond;
        }
    }

    public static class ReturnStat extends Stat {
        public String name;
        public ReturnStat(String name) {
            this.name = name;
        }
    }
}
