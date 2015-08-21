package edu.clemson.resolve.misc;

import edu.clemson.resolve.parser.ResolveLexer;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;

public abstract class HardCodedProgOps {

    public CommonToken qualifier, name;

    public HardCodedProgOps(Token op) {
        this.name = new CommonToken(op);
    }

    public HardCodedProgOps(String qualifier, Token original, String opAsText) {
        this.qualifier = qualifier;
        this.name = name;
    }

    public BuiltInOpAttributes convert(Token op) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op); //default answer (same thing that comes in)
        switch (op.getType()) {
            case ResolveLexer.EQUALS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Sum");
                break;
            case ResolveLexer.NEQUALS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Difference");
                break;
        }
        return null;
    }

    public static class IntegerHardCodedProgOps extends HardCodedProgOps {

        public IntegerHardCodedProgOps() {
            super("Std_Integer_Fac", new CommonToken(original), original.getText());
            this.name = new CommonToken(original);
            this.name.setText(opAsText);
            this.qualifier = new CommonToken(original);
            this.qualifier.setText(qualifier);
        }
    }

    public static class BuiltInOpAttributes {
        public CommonToken qualifier, name;

        public BuiltInOpAttributes(Token op) {
            this.name = new CommonToken(op);
        }

        public BuiltInOpAttributes(String qualifier, Token original,
                                   String opAsText) {
            this.name = new CommonToken(original);
            this.name.setText(opAsText);
            this.qualifier = new CommonToken(original);
            this.qualifier.setText(qualifier);
        }
    }
}
