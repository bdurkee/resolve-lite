package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.typereasoning.TypeGraph;
import org.rsrg.semantics.SymbolTable;

public class ModelBuilderProto extends ResolveBaseListener {

    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final TypeGraph g;

    public static final StatRuleApplicationStrategy EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy();
    private final static StatRuleApplicationStrategy FUNCTION_ASSIGN_APPLICATION =
            new FunctionAssignApplicationStrategy();
    private final static StatRuleApplicationStrategy SWAP_APPLICATION =
            new SwapApplicationStrategy();
}