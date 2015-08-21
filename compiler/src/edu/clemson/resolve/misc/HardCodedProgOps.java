package edu.clemson.resolve.misc;

import edu.clemson.resolve.parser.ResolveLexer;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import org.rsrg.semantics.programtype.PTNamed;
import org.rsrg.semantics.programtype.PTType;

import java.util.Arrays;
import java.util.List;

public class HardCodedProgOps {

    public static BuiltInOpAttributes convert(Token op, PTType ... args) {
        return convert(op, Arrays.asList(args));
    }

    public static BuiltInOpAttributes convert(Token op, List<PTType> args) {
        if (args.isEmpty()) {
            return convertBooleanProgramOp(op); //only thing we could possibly match that has no arguments is true or false
        }
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);
        if (!(args.get(0) instanceof PTNamed)) return result;
        PTNamed firstArgType = (PTNamed)args.get(0);
        if ( firstArgType.getName().equals("Boolean") ) {
            result = convertBooleanProgramOp(op);
        }
        else if ( firstArgType.getName().equals("Integer") ) {
            result = convertIntegerProgramOp(op);
        }
        else if ( firstArgType.getName().equals("Char_Str") ) {
            result = convertCharStrProgramOp(op);
        }
        return result;
    }

    public static BuiltInOpAttributes convertCharStrProgramOp(Token op) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);
        switch (op.getType()) {
            case ResolveLexer.PLUSPLUS:
                result = new BuiltInOpAttributes("Std_Char_Str_Fac", op, "Merger");
                break;
        }
        return result;
    }

    public static BuiltInOpAttributes convertIntegerProgramOp(Token op) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);
        switch (op.getType()) {
            case ResolveLexer.PLUS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Sum");
                break;
            case ResolveLexer.MINUS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Difference");
                break;
            case ResolveLexer.MULT:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Product");
                break;
            case ResolveLexer.DIVIDE:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Divide");
                break;
            case ResolveLexer.LTE:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Less_Or_Equal");
                break;
            case ResolveLexer.LT:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Less");
                break;
            case ResolveLexer.GTE:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Greater_Or_Equal");
                break;
            case ResolveLexer.GT:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Greater");
                break;
            case ResolveLexer.EQUALS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Are_Equal");
                break;
            case ResolveLexer.NEQUALS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Are_Not_Equal");
                break;
            case ResolveLexer.MINUSMINUS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Decrement");
                break;
            case ResolveLexer.PLUSPLUS:
                result = new BuiltInOpAttributes("Std_Integer_Fac", op, "Increment");
                break;
        }
        return result;
    }

    public static BuiltInOpAttributes convertBooleanProgramOp(Token op) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);
        switch (op.getType()) {
            case ResolveLexer.NOT:
                result = new BuiltInOpAttributes("Std_Boolean_Fac", op, "Not");
                break;
            case ResolveLexer.EQUALS:
                result = new BuiltInOpAttributes("Std_Boolean_Fac", op, "Are_Equal");
                break;
            case ResolveLexer.NEQUALS:
                result = new BuiltInOpAttributes("Std_Boolean_Fac", op, "Are_Not_Equal");
                break;
            case ResolveLexer.TRUE:
                result = new BuiltInOpAttributes("Std_Boolean_Fac", op, "True");
                break;
            case ResolveLexer.FALSE:
                result = new BuiltInOpAttributes("Std_Boolean_Fac", op, "False");
                break;
            case ResolveLexer.AND:
                result = new BuiltInOpAttributes("Std_Boolean_Fac", op, "And");
                break;
            case ResolveLexer.OR:
                result = new BuiltInOpAttributes("Std_Boolean_Fac", op, "Or");
                break;
        }
        return result;
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
