package concepts.standard;

import org.resolvelite.runtime.RType;
import org.resolvelite.runtime.ResolveBase;

public class Boolean_Impl extends ResolveBase implements Boolean_Template {

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

    @Override public RType createBoolean() {
        return new Boolean();
    }

    @Override public Boolean_Template.Boolean createBoolean(boolean b) {
        return new Boolean(b);
    }

    @Override public RType True() {
        return new Boolean(true);
    }

    @Override public RType False() {
        return new Boolean(false);
    }

    @Override public RType And(RType a, RType b) {
        return new Boolean(((Boolean_Impl.Boolean)a).val &&
                ((Boolean_Impl.Boolean)b).val);
    }

    @Override public void assign(RType r1, RType r2) {

    }
}
