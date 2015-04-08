import org.resolvelite.runtime.*;
import java.lang.reflect.*;

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
        int content;
        Integer_Rep() {
            content = 0;
        }
        Integer_Rep(int i) {
            content = i;
        }

        @Override public String toString() {
            return String.valueOf(content);
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

    public RType Is_Zero(RType i) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer) i)
                .rep.content == 0);
    }

    public RType Sum(RType i1, RType i2) {
        return new Integer(((Integer_Impl.Integer)i1).rep.content +
                ((Integer_Impl.Integer)i2).rep.content);
    }

    public RType Difference(RType i1, RType i2) {
        return new Integer(((Integer_Impl.Integer)i1).rep.content -
                ((Integer_Impl.Integer)i2).rep.content);
    }
}