package edu.clemson.resolve.proving.absyn;

import edu.clemson.resolve.misc.Utils;
import org.rsrg.semantics.MTType;

import java.util.*;

public class PApply extends PExp {

    private final PExp functionPortion;
    private final List<PExp> arguments = new ArrayList<>();

    public PApply(PApplyBuilder builder) {
        super(calculateHashes(builder.functionPortion,
                        builder.arguments.iterator()), builder.mathType,
                builder.mathTypeValue);
        this.functionPortion = builder.functionPortion;
        this.arguments.addAll(builder.arguments);
    }

    @Override protected void splitIntoConjuncts(List<PExp> accumulator) {
        if (arguments.size() == 2 &&
                functionPortion.getCanonicalizedName().equals("and")) {
            arguments.get(0).splitIntoConjuncts(accumulator);
            arguments.get(1).splitIntoConjuncts(accumulator);
        }
        else {
            accumulator.add(this);
        }
    }

    protected static HashDuple calculateHashes(PExp functionPortion,
                                               Iterator<PExp> args) {
        int structureHash = 0;
        int valueHash = 0;
        if (functionPortion != null) {
            valueHash = functionPortion.valueHash;
        }
        if ( args.hasNext() ) {
            structureHash = 17;
            int argMod = 2;
            PExp arg;
            while (args.hasNext()) {
                arg = args.next();
                structureHash += arg.structureHash * argMod;
                valueHash += arg.valueHash * argMod;
                argMod++;
            }
        }
        return new HashDuple(structureHash, valueHash);
    }

    public static class PApplyBuilder implements Utils.Builder<PApply> {
        protected final PExp functionPortion;
        protected final List<PExp> arguments = new ArrayList<>();
        protected MTType mathType, mathTypeValue;

        public PApplyBuilder(PExp functionPortion) {
            this.functionPortion = functionPortion;
        }

        public PApplyBuilder mathType(MTType type) {
            this.mathType = type;
            return this;
        }

        public PApplyBuilder mathTypeValue(MTType typeValue) {
            this.mathTypeValue = typeValue;
            return this;
        }

        public PApplyBuilder arguments(PExp ... args) {
            arguments(Arrays.asList(args));
            return this;
        }

        public PApplyBuilder arguments(Collection<PExp> args) {
            arguments.addAll(args);
            return this;
        }

        @Override public PApply build() {
            return new PApply(this);
        }
    }
}
