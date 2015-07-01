package edu.clemson.resolve.compiler;

import edu.clemson.resolve.misc.Utils;
import edu.clemson.resolve.parser.Resolve;
import edu.clemson.resolve.parser.ResolveBaseListener;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;

import edu.clemson.resolve.compiler.ImportCollection.ImportType;

import java.util.*;

/**
 * Fills in the contents of an {@link ImportCollection} by visiting the
 * various {@link ParseTree} nodes that reference other modules.
 */
public class ImportListener extends ResolveBaseListener {

    private final ImportCollection importCollection = new ImportCollection();

    @NotNull public ImportCollection getImports() {
        return importCollection;
    }
    public static final Map<String, LinkedHashSet<String>> STD_MODULES =
            new HashMap<>();

    public static final List<String> DEFAULT_IMPORTS = Collections
            .unmodifiableList(Arrays.asList("Standard_Booleans",
                    "Standard_Integers"));
    static {
        //first param defines a special, "default module", all proceeding params
        //are things it will automatically import.
        registerStandardModule("Boolean_Template");
        registerStandardModule("Standard_Booleans");
        registerStandardModule("Integer_Template", "Standard_Booleans");
        registerStandardModule("Standard_Integers");
    }

    protected static void registerStandardModule(String moduleName) {
        STD_MODULES.put(moduleName, new LinkedHashSet<>());
    }

    protected static void registerStandardModule(String moduleName,
                                        String... defaultImports) {
        STD_MODULES.put(moduleName,
                new LinkedHashSet<>(Arrays.asList(defaultImports)));
    }

    protected static void registerStandardModule(String moduleName,
                                        LinkedHashSet<String> defaultImports) {
        STD_MODULES.put(moduleName, defaultImports);
    }

    @Override public void enterModule(@NotNull Resolve.ModuleContext ctx) {
        ParseTree moduleChild = ctx.getChild(0);
        if ( !(moduleChild instanceof Resolve.PrecisModuleContext) ) {
            LinkedHashSet<String> stdImports =
                    STD_MODULES.get(Utils.getModuleName(moduleChild));
            if ( stdImports != null ) { // if this is a standard module
                importCollection.addTokenSet(ImportType.NAMED, stdImports);
            }
            else {
                importCollection.addTokenSet(ImportType.NAMED, DEFAULT_IMPORTS);
            }
        }
    }

    @Override public void enterConceptImplModule(
            @NotNull Resolve.ConceptImplModuleContext ctx) {
        importCollection.imports(ImportType.NAMED, ctx.concept.getText());
    }

    @Override public void enterEnhancementModule(
            @NotNull Resolve.EnhancementModuleContext ctx) {
        importCollection.imports(ImportType.NAMED, ctx.concept.getText());
    }

    @Override public void enterEnhancementImplModule(
            @NotNull Resolve.EnhancementImplModuleContext ctx) {
        importCollection.imports(ImportType.NAMED, ctx.enhancement.getText(),
                ctx.concept.getText());
    }

    @Override public void exitUsesList(
            @NotNull Resolve.UsesListContext ctx) {
        importCollection.imports(ImportType.NAMED, ctx.ID());
    }

    @Override public void exitFacilityDecl(
            @NotNull Resolve.FacilityDeclContext ctx) {
        importCollection.imports(ImportType.IMPLICIT, ctx.spec.getText());
        ImportCollection.ImportType type =
                (ctx.externally != null) ? ImportType.EXTERNAL
                        : ImportType.IMPLICIT;
        importCollection.imports(type, ctx.impl.getText());
    }
}