package edu.clemson.resolve.vcgen;

import edu.clemson.resolve.compiler.AnnotatedTree;
import edu.clemson.resolve.parser.ResolveBaseListener;
import edu.clemson.resolve.typereasoning.TypeGraph;
import edu.clemson.resolve.vcgen.model.VCOutputFile;
import edu.clemson.resolve.vcgen.model.VCAssertiveBlock.VCAssertiveBlockBuilder;
import org.rsrg.semantics.SymbolTable;

import java.util.Deque;
import java.util.LinkedList;

public class ModelBuilderProto extends ResolveBaseListener {

    private final AnnotatedTree tr;
    private final SymbolTable symtab;
    private final TypeGraph g;

  /*  public static final StatRuleApplicationStrategy EXPLICIT_CALL_APPLICATION =
            new ExplicitCallApplicationStrategy();
    private final static StatRuleApplicationStrategy FUNCTION_ASSIGN_APPLICATION =
            new FunctionAssignApplicationStrategy();
    private final static StatRuleApplicationStrategy SWAP_APPLICATION =
            new SwapApplicationStrategy();*/
    private final VCOutputFile outputFile = new VCOutputFile();

    private final Deque<VCAssertiveBlockBuilder> assertiveBlocks =
            new LinkedList<>();
    public ModelBuilderProto(VCGenerator gen, SymbolTable symtab) {
        this.symtab = symtab;
        this.tr = gen.getModule();
        this.g = symtab.getTypeGraph();
    }

    public VCOutputFile getOutputFile() {
        return outputFile;
    }
}