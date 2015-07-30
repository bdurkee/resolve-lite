import java.util.Scanner;

public class Char_Str_Impl extends RESOLVEBase implements Char_Str_Template {

    public class Char_Str implements Char_Str_Template.Char_Str {
        public String val;

        Char_Str() {
            val = " ";
        }

        Char_Str(String s) {
            val = s;
        }

        public Object getRep() {
            return val;
        }

        public void setRep(Object o) {
            val = (String)o;
        }

        public RType initialValue() {
            return new Char_Str();
        }

        public String toString() {
            return val;
        }
    }

    @Override public RType initChar_Str(String ... s) {
        if (s.length >= 1) {
            return new Char_Str(s[0]);
        }
        else {
            return new Char_Str();
        }
    }

    @Override public RType Are_Equal(RType s1, RType s2) {
        String js1 = ((Char_Str)s1).val;
        String js2 = ((Char_Str)s2).val;
        return Standard_Booleans.INSTANCE.initBoolean(js1.equals(js2));
    }

    @Override public RType Are_Not_Equal(RType s1, RType s2) {
        String js1 = ((Char_Str)s1).val;
        String js2 = ((Char_Str)s2).val;
        return Standard_Booleans.INSTANCE.initBoolean(!js1.equals(js2));
    }

    @Override public RType Merger(RType s1, RType s2) {
        String js1 = ((Char_Str)s1).val;
        String js2 = ((Char_Str)s2).val;
        js1 = js1 + js2;
        return new Char_Str(js1);
    }

    @Override public RType Length(RType s) {
        return Standard_Integers.INSTANCE
                .initInteger(((Char_Str) s).val.length());
    }

    @Override public void Read(RType s) {
        Scanner sc = new Scanner(System.in);
        ((Char_Str)s).val = sc.nextLine();
    }

    @Override public void Write(RType s) {
        System.out.print(((Char_Str) s).val);
    }

    @Override public void Write_Line(RType s) {
        System.out.println(((Char_Str) s).val);
    }
}
