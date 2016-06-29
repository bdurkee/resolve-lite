package edu.clemson.resolve.misc;

import edu.clemson.resolve.parser.ResolveLexer;
import org.antlr.v4.runtime.CommonToken;
import org.antlr.v4.runtime.Token;
import edu.clemson.resolve.semantics.programtype.ProgNamedType;
import edu.clemson.resolve.semantics.programtype.ProgType;

import java.util.Arrays;
import java.util.List;

public class StdTemplateProgOps {

    public static BuiltInOpAttributes convert(Token op, ProgType... args) {
        return convert(op, Arrays.asList(args));
    }

    public static boolean isBuiltinOp(Token op) {
        con
        return false;
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
        switch (op.getText()) {
            case "+":
                result = new BuiltInOpAttributes("Std_Ints", op, "Sum");
                break;
            case "-":
                result = new BuiltInOpAttributes("Std_Ints", op, "Difference");
                break;
            case "*":
                result = new BuiltInOpAttributes("Std_Ints", op, "Product");
                break;
            case "/":
                result = new BuiltInOpAttributes("Std_Ints", op, "Divide");
                break;
            case "<=":
                result = new BuiltInOpAttributes("Std_Ints", op, "Less_Or_Equal");
                break;
            case "<":
                result = new BuiltInOpAttributes("Std_Ints", op, "Less");
                break;
            case ">=":
                result = new BuiltInOpAttributes("Std_Ints", op, "Greater_Or_Equal");
                break;
            case ">":
                result = new BuiltInOpAttributes("Std_Ints", op, "Greater");
                break;
            case "=":
                result = new BuiltInOpAttributes("Std_Ints", op, "Are_Equal");
                break;
            case "/=":
                result = new BuiltInOpAttributes("Std_Ints", op, "Are_Not_Equal");
                break;
            case "%":
                result = new BuiltInOpAttributes("Std_Ints", op, "Mod");
                break;
        }
        return result;
    }

    public static BuiltInOpAttributes convertBooleanProgramOp(Token op) {
        BuiltInOpAttributes result = new BuiltInOpAttributes(op);
        switch (op.getText()) {
            case "not":
                result = new BuiltInOpAttributes("Std_Bools", op, "Not");
                break;
            case "=":
                result = new BuiltInOpAttributes("Std_Bools", op, "Are_Equal");
                break;
            case "/=":
                result = new BuiltInOpAttributes("Std_Bools", op, "Are_Not_Equal");
                break;
            case "true":
                result = new BuiltInOpAttributes("Std_Bools", op, "True");
                break;
            case "false":
                result = new BuiltInOpAttributes("Std_Bools", op, "False");
                break;
            case "and":
                result = new BuiltInOpAttributes("Std_Bools", op, "And");
                break;
            case "or":
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

        public BuiltInOpAttributes(String qualifier, Token original, String opAsText) {
            this.name = new CommonToken(original);
            this.name.setText(opAsText);
            this.qualifier = new CommonToken(original);
            this.qualifier.setText(qualifier);
        }
    }
}
