package concepts.integer_template;

import edu.clemson.resolve.runtime.RESOLVEBase;
import edu.clemson.resolve.runtime.RType;

import java.util.Scanner;
import java.lang.reflect.*;
import edu.clemson.resolve.runtime.*;

public class Standard_Integer_Impl extends RESOLVEBase implements Integer_Template {

    public class Integer implements Integer_Template.Integer {
        int val;
        Integer() {
            val = 0;
        }

        Integer(int i) {
            val = i;
        }

        // getRep is special case, this will never be called
        public Object getRep() {
            return new Integer(val);
        }

        // setRep is special case, this will never be called
        public void setRep(Object o) {
            val = ((Integer)o).val;
        }

        public RType initialValue() {
            return new Integer();
        }

        public String toString() {
            return new java.lang.Integer(val).toString();
        }
    }

    public RType initInteger(int ... e) {
        if (e.length >= 1) {
            return new Integer(e[0]);
        }
        else {
            return new Integer();
        }
    }

    /*@Override public RType Is_Zero(RType i) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i).val == 0);
    }

    @Override public RType Is_Not_Zero(RType i) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i).val != 0);
    }*/

    @Override public void Increment(RType i) {
        ((Integer)i).val = ((Integer)i).val + 1;
    }

    @Override public void Decrement(RType i) {
        ((Integer)i).val = ((Integer)i).val - 1;
    }

    @Override public RType Are_Equal(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer)i1).val ==
                ((Integer)i2).val);
    }

    @Override public RType Are_Not_Equal(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).val !=
                ((Integer) i2).val);
    }

    @Override public RType Less_Or_Equal(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).val <=
                ((Integer) i2).val);
    }

    @Override public RType Less(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).val <
                ((Integer) i2).val);
    }

    @Override public RType Greater(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).val >
                ((Integer) i2).val);
    }

    @Override public RType Greater_Or_Equal(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).val >=
                ((Integer) i2).val);
    }

    @Override public RType Sum(RType i1, RType i2) {
        return new Integer(((Integer)i1).val +
                ((Integer)i2).val);
    }

    @Override public RType Difference(RType i1, RType i2) {
        return new Integer(((Integer)i1).val -
                ((Integer)i2).val);
    }

    @Override public RType Product(RType i1, RType i2) {
        return new Integer(((Integer)i1).val * ((Integer)i2).val);
    }

    @Override public RType Negate(RType i1) {
        return new Integer(-((Integer)i1).val);
    }

    @Override public void Write(RType i) {
        System.out.print(((Integer) i).val);
    }

    @Override public void Write_Line(RType i) {
        System.out.println(((Integer) i).val);
    }

    @Override public void Read(RType e) {
        Scanner sc = new Scanner(System.in);
        ((Integer)e).val = sc.nextInt();
    }
}