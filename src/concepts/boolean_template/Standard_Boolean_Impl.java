package concepts.boolean_template;

import edu.clemson.resolve.runtime.*;
import java.lang.reflect.*;

import java.util.Scanner;

public class Standard_Boolean_Impl extends RESOLVEBase implements Boolean_Template{

    public class Boolean implements Boolean_Template.Boolean {
        public boolean val;

        Boolean() {
            val = true;
        }

        Boolean(boolean i) {
            val = i;
        }

        public Object getRep() {
            return new Boolean(val);
        }

        public void setRep(Object o) {
        }

        public RType initialValue() {
            return new Boolean();
        }

        public String toString() {
            return new java.lang.Boolean(val).toString();
        }
    }

    public RType replica(RType b) {
        return new Boolean(((Boolean)b).val);
    }

    public RType initBoolean(boolean ... e) {
        if (e.length >= 1) {
            return new Boolean(e[0]);
        }
        else {
            return new Boolean();
        }
    }

    @Override
    public RType True() {
        return new Boolean(true);
    }

    @Override
    public RType False() {
        return new Boolean(false);
    }

    @Override
    public RType And(RType a, RType b) {
        return new Boolean(((Boolean)a).val && ((Boolean)b).val);
    }

    @Override
    public RType Or(RType a, RType b) {
        return new Boolean(((Boolean)b).val || ((Boolean)b).val);
    }

    @Override
    public RType Not(RType a) {
        return new Boolean(!((Boolean)a).val);
    }

    @Override
    public RType Are_Equal(RType a, RType b) {
        return new Boolean(((Boolean)a).val == ((Boolean)b).val);
    }

    @Override
    public RType Are_Not_Equal(RType a, RType b) {
        return new Boolean(((Boolean)a).val != ((Boolean)b).val);
    }

    @Override
    public void Write(RType i) {
        System.out.print(((Boolean) i).val);
    }

    @Override
    public void Write_Line(RType i) {
        System.out.println(((Boolean) i).val);
    }
/*
@Override
public void Read(RType e) {
    Scanner sc = new Scanner(System.in);
    ((Boolean)e).rep.val = sc.nextBoolean();
}*/
}