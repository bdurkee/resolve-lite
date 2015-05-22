import org.resolvelite.runtime.*;
import java.lang.reflect.*;

import java.util.Scanner;

public class Integer_Impl extends ResolveBase implements Integer_Template {

    public class Integer implements Integer_Template.Integer {
        Integer_Rep rep;
        Integer() {
            rep = new Integer_Rep();
        }

        Integer(int i) {
            rep = new Integer_Rep(i);
        }

        // getRep is special case, this will never be called
        public Object getRep() {
            return rep;
        }

        // setRep is special case, this will never be called
        public void setRep(Object o) {
            rep = (Integer_Rep)o;
        }

        public RType initialValue() {
            return new Integer();
        }

        public String toString() {
            return rep.toString();
        }
    }
    class Integer_Rep {
        int val;
        Integer_Rep() {
            val = 0;
        }
        Integer_Rep(int i) {
            val = i;
        }

        @Override public String toString() {
            return String.valueOf(val);
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

    @Override public RType Is_Zero(RType i) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i)
                .rep.val == 0);
    }

    @Override public RType Is_Not_Zero(RType i) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i)
                .rep.val != 0);
    }

    @Override public void Increment(RType i) {
        ((Integer)i).rep.val = ((Integer)i).rep.val + 1;
    }

    @Override public void Decrement(RType i) {
        ((Integer)i).rep.val = ((Integer)i).rep.val - 1;
    }

    @Override public RType Are_Equal(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer)i1).rep.val ==
                ((Integer)i2).rep.val);
    }

    @Override public RType Are_Not_Equal(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).rep.val !=
                ((Integer) i2).rep.val);
    }

    @Override public RType Less_Or_Equal(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).rep.val <=
                ((Integer) i2).rep.val);
    }

    @Override public RType Less(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).rep.val <
                ((Integer) i2).rep.val);
    }

    @Override public RType Greater(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).rep.val >
                ((Integer) i2).rep.val);
    }

    @Override public RType Greater_Or_Equal(RType i1, RType i2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i1).rep.val >=
                ((Integer) i2).rep.val);
    }

    @Override public RType Sum(RType i1, RType i2) {
        return new Integer(((Integer)i1).rep.val +
                ((Integer)i2).rep.val);
    }

    @Override public RType Difference(RType i1, RType i2) {
        return new Integer(((Integer)i1).rep.val -
                ((Integer)i2).rep.val);
    }

    @Override public RType Product(RType i1, RType i2) {
        return new Integer(((Integer)i1).rep.val * ((Integer)i2).rep.val);
    }

    @Override public void Divide(RType i, RType j, RType q) {
        ((Integer)i).rep.val = (((Integer)i).rep.val / ((Integer)j).rep.val);
    }

    @Override public RType Negate(RType i1) {
        return new Integer(-((Integer)i1).rep.val);
    }

    @Override public void Read(RType e) {
        Scanner sc = new Scanner(System.in);
        ((Integer)e).rep.val = sc.nextInt();
    }

    @Override public void Write_Line(RType i) {
        System.out.println(((Integer) i).rep.val);
    }
}