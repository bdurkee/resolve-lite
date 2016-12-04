package concepts.char_str_template;

import edu.clemson.resolve.runtime.RESOLVEBase;
import edu.clemson.resolve.runtime.RType;

import facilities.Standard_Booleans;
import facilities.Standard_Integers;
import java.util.Scanner;
import java.lang.reflect.*;
import edu.clemson.resolve.runtime.*;

public class Standard_Char_Str_Realiz extends RESOLVEBase implements Char_Str_Template {

    public class Char_Str implements Char_Str_Template.Char_Str {
        public String val;
        
        Char_Str() {
            val = "";
        }

        Char_Str(String s) {
            val = s;
        }

        // getRep is special case, this will never be called
        public Object getRep() {
            return val;
        }

        // setRep is special case, this will never be called
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

    public RType initChar_Str(String ... e) {
        if (e.length >= 1) {
            return new Char_Str(e[0]);
        }
        else {
            return new Char_Str();
        }
    }
    
    @Override
    public RType Are_Equal(RType s1, RType s2) {
        String js1 = ((Char_Str)s1).val;
        String js2 = ((Char_Str)s2).val;
        return Standard_Booleans.Std_Bools.initBoolean(js1.equals(js2));
    }
    
    @Override
    public RType Are_Not_Equal(RType s1, RType s2) {
        String js1 = ((Char_Str)s1).val;
        String js2 = ((Char_Str)s2).val;
        return Standard_Booleans.Std_Bools.initBoolean(!js1.equals(js2));
    }
    
    @Override
    public RType Length(RType s) {
        String js = ((Char_Str)s).val;
        int length = js.length();
        return Standard_Integers.Std_Ints.initInteger(length);
    }
    
    @Override
    public void Write(RType s) {
        System.out.print(((Char_Str) s).val);
    }
    
    @Override
    public void Write_Line(RType s) {
        System.out.println(((Char_Str) s).val);
    }
    
/*
    @Override public void Read(RType e) {
        Scanner sc = new Scanner(System.in);
        ((Integer)e).val = sc.nextInt();
    }*/
}
