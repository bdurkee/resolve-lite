import org.resolvelite.runtime.*;
import java.lang.reflect.*;

public class Boolean_Impl extends ResolveBase implements Boolean_Template{

    public class Boolean implements Boolean_Template.Boolean {
        public boolean val;

        Boolean() {
            val = true;
        }

        Boolean(boolean b) {
            val = b;
        }

        // getRep is special case, this will never be called
        public Object getRep() {
            return this;
        }

        // setRep is special case, this will never be called
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
        return new Boolean(((Boolean_Impl.Boolean)b).val);
    }

    public RType initBoolean(boolean ... e) {
        if (e.length >= 1) {
            return new Boolean(e[0]);
        }
        else {
            return new Boolean();
        }
    }

    public RType And(RType b1, RType b2) {
        return new Boolean(((Boolean_Impl.Boolean)b1).val &&
                ((Boolean_Impl.Boolean)b2).val);
    }

    public RType Or(RType b1, RType b2) {
        return new Boolean(((Boolean_Impl.Boolean)b1).val ||
                ((Boolean_Impl.Boolean)b2).val);
    }

    public RType True() {
        return new Boolean(true);
    }

    public RType False() {
        return new Boolean(false);
    }

}
