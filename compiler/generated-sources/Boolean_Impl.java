import java.lang.reflect.*;

import java.util.Scanner;

public class Boolean_Impl 
		extends 
			RESOLVEBase implements Boolean_Template {

    public class Boolean 
    		implements 
    			Boolean_Template.Boolean {
        public Boolean_Rep rep;

        Boolean() {
            rep = new Boolean_Rep();
        }

        Boolean(boolean i) {
            rep = new Boolean_Rep(i);
        }

        public Object getRep() {
            return rep;
        }

        public void setRep(Object o) {
            rep = (Boolean_Rep)o;
        }

        public RType initialValue() {
            return new Boolean();
        }

        public String toString() {
            return rep.toString();
        }
    }
    class Boolean_Rep {
        boolean val;
        Boolean_Rep() {
            val = true;
        }
        Boolean_Rep(boolean e) {
            val = e;
        }
        @Override public String toString() {
            return String.valueOf(val);
        }
    }

    public RType replica(RType b) {
        return new Boolean(((Boolean)b).rep.val);
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
        return new Boolean(((Boolean)b1).rep.val && ((Boolean)b2).rep.val);
    }

    @Override public RType Or(RType b1, RType b2) {
        return new Boolean(((Boolean)b1).rep.val || ((Boolean)b2).rep.val);
    }

    @Override public RType Not(RType b) {
        return new Boolean(!((Boolean)b).rep.val);
    }

    @Override public void Read(RType e) {
        Scanner sc = new Scanner(System.in);
        ((Boolean)e).rep.val = sc.nextBoolean();
    }

    @Override public void Write(RType i) {
        System.out.print(((Boolean) i).rep.val);
    }

    @Override public void Write_Line(RType i) {
        System.out.println(((Boolean) i).rep.val);
    }
}
