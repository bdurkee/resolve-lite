import org.resolvelite.runtime.*;
import java.lang.reflect.*;

public class Integer_Impl extends ResolveBase implements Integer_Template {

    public class Integer implements Integer_Template.Integer {
        public int val;

        Integer() {
            val = 0;
        }

        Integer(int i) {
            val = i;
        }

        // getRep is special case, this will never be called
        public Object getRep() {
            return this;
        }

        // setRep is special case, this will never be called
        public void setRep(Object o) {
        }

        public RType initialValue() {
            return new Integer();
        }

        public String toString() {
            return new java.lang.Integer(val).toString();
        }
    }

    public RType initInteger(int ... e) {
        if (e.length > 1) {
            return new Integer(e[0]);
        }
        else {
            return new Integer();
        }
    }

    public RType Is_Zero(RType i) {
        return Standard_Booleans.INSTANCE.initBoolean(((Integer_Impl.Integer) i).val == 0);
    }
}