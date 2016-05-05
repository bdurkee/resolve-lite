package edu.clemson.resolve.misc;

import edu.clemson.resolve.parser.ResolveLexer;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import edu.clemson.resolve.semantics.programtype.ProgNamedType;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.Arrays;
import java.util.List;

public class HardCodedProgOps {

    public static BuiltInOpAttributes convert(Token op, ProgType... args) {
        return convert(op, Arrays.asList(args));
    }

    public static BuiltInOpAttributes convert(Token op, List<ProgType> args) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);

        if (args.isEmpty()) {
            return convertBooleanProgramOp(op); //only (pseudo hardcoded) thing we could possibly match currently that has no arguments is true or false
        }
        if (!(args.get(0) instanceof ProgNamedType)) return result;
        ProgNamedType firstArgType = (ProgNamedType) args.get(0);

        if (firstArgType.getName().equals("Boolean")) {
            result = convertBooleanProgramOp(op);
        }
        else if (firstArgType.getName().equals("Integer")) {
            result = convertIntegerProgramOp(op);
        }
        else if (firstArgType.getName().equals("Char_Str")) {
            result = convertCharStrProgramOp(op);
        }
        return result;
    }

    public static BuiltInOpAttributes convertCharStrProgramOp(Token op) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);
       /* switch (op.getType()) {
            case ResolveLexer.PLUSPLUS:
                result = new BuiltInOpAttributes("Std_Char_Str_Fac", op, "Merger");
                break;
        }*/
        return result;
    }

    public static BuiltInOpAttributes convertIntegerProgramOp(Token op) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);
        switch (op.getType()) {
            case ResolveLexer.PLUS:
                result = new BuiltInOpAttributes("Std_Ints", op, "Sum");
                break;
            case ResolveLexer.MINUS:
                result = new BuiltInOpAttributes("Std_Ints", op, "Difference");
                break;
            case ResolveLexer.MULT:
                result = new BuiltInOpAttributes("Std_Ints", op, "Product");
                break;
            case ResolveLexer.DIV:
                result = new BuiltInOpAttributes("Std_Ints", op, "Divide");
                break;
            case ResolveLexer.LTE:
                result = new BuiltInOpAttributes("Std_Ints", op, "Less_Or_Equal");
                break;
            case ResolveLexer.LT:
                result = new BuiltInOpAttributes("Std_Ints", op, "Less");
                break;
            case ResolveLexer.GTE:
                result = new BuiltInOpAttributes("Std_Ints", op, "Greater_Or_Equal");
                break;
            case ResolveLexer.GT:
                result = new BuiltInOpAttributes("Std_Ints", op, "Greater");
                break;
            case ResolveLexer.EQUALS:
                result = new BuiltInOpAttributes("Std_Ints", op, "Are_Equal");
                break;
            case ResolveLexer.NEQUALS:
                result = new BuiltInOpAttributes("Std_Ints", op, "Are_Not_Equal");
                break;
            case ResolveLexer.MOD:
                result = new BuiltInOpAttributes("Std_Ints", op, "Mod");
                break;
        }
        return result;
    }

    public static BuiltInOpAttributes convertBooleanProgramOp(Token op) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);
        switch (op.getType()) {
            case ResolveLexer.NOT:
                result = new BuiltInOpAttributes("Std_Bools", op, "Not");
                break;
            case ResolveLexer.EQUALS:
                result = new BuiltInOpAttributes("Std_Bools", op, "Are_Equal");
                break;
            case ResolveLexer.NEQUALS:
                result = new BuiltInOpAttributes("Std_Bools", op, "Are_Not_Equal");
                break;
            case ResolveLexer.TRUE:
                result = new BuiltInOpAttributes("Std_Bools", op, "True");
                break;
            case ResolveLexer.FALSE:
                result = new BuiltInOpAttributes("Std_Bools", op, "False");
                break;
            case ResolveLexer.AND:
                result = new BuiltInOpAttributes("Std_Bools", op, "And");
                break;
            case ResolveLexer.OR:
                result = new BuiltInOpAttributes("Std_Bools", op, "Or");
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
