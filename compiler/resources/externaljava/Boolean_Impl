import java.lang.reflect.*;

import java.util.Scanner;

import edu.clemson.resolve.runtime.*;

public class Boolean_Impl 
		extends 
			RESOLVEBase implements Boolean_Template {

    public class Boolean 
    		implements 
    			Boolean_Template.Boolean {
        public boolean val;

        Boolean() {
            val = false;
        }

        Boolean(boolean i) {
            val = i;
        }

        public Object getRep() {
            return new Boolean(val);
        }

        public void setRep(Object o) {
            val = ((Boolean)o).val;
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

    @Override public RType True() {
        return new Boolean(true);
    }

    @Override public RType False() {
        return new Boolean(false);
    }

    @Override public RType And(RType b1, RType b2) {
        return new Boolean(((Boolean)b1).val && ((Boolean)b2).val);
    }

    @Override public RType Or(RType b1, RType b2) {
        return new Boolean(((Boolean)b1).val || ((Boolean)b2).val);
    }

    @Override public RType Not(RType b) {
        return new Boolean(!((Boolean)b).val);
    }

    @Override public RType Are_Equal(RType b1, RType b2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Boolean)b1).val ==
                ((Boolean)b2).val);
    }

    @Override public RType Are_Not_Equal(RType b1, RType b2) {
        return Standard_Booleans.INSTANCE.initBoolean(((Boolean)b1).val !=
                ((Boolean)b2).val);
    }

    @Override public void Read(RType e) {
        Scanner sc = new Scanner(System.in);
        ((Boolean)e).val = sc.nextBoolean();
    }

    @Override public void Write(RType i) {
        System.out.print(((Boolean) i).val);
    }

    @Override public void Write_Line(RType i) {
        System.out.println(((Boolean) i).val);
    }
}
