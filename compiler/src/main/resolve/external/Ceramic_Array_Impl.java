public class Ceramic_Array_Impl extends ResolveBase
        implements
        Bdd_Ceramic_Array_Template {
    RType type;
    int lowerBound, upperBound;

    class Array extends ResolveBase implements RType {
        Array_Rep rep;
        Array() {
            rep = new Array_Rep();
        }

        @Override public Object getRep() {
            return rep;
        }

        @Override public void setRep(Object o) {
            rep = (Array_Rep)o;
        }

        @Override public String toString() {
            return rep.toString();
        }

        @Override public RType initialValue() {
            return new Array();
        }
    }
    class Array_Rep {
        RType[] content;
        Array_Rep() {
            content = new RType[upperBound - lowerBound + 1];
            for(int i = 0; i < content.length; i++) {
                content[i] = type.initialValue();
            }
        }
        @Override public String toString() {
            StringBuilder sb = new StringBuilder();
            for (RType aContent : content) {
                sb.append(aContent.toString());
                sb.append("\n");
            }
            return sb.toString();
        }
    }

    @Override public RType initCeramic_Array() {
        return new Array();
    }

    public Ceramic_Array_Impl(RType type, RType Lower_Bound, RType Upper_Bound) {
        this.type = type;
        this.lowerBound = ((Integer_Impl.Integer)Lower_Bound).rep.val;
        this.upperBound = ((Integer_Impl.Integer)Upper_Bound).rep.val;
    }

    @Override public void Swap_Element(RType A, RType e, RType i) {
        int adj = ((Integer_Impl.Integer)i).rep.val - lowerBound;
        RType[] temp1 = ((Array_Rep)A.getRep()).content;
        swap(e, temp1[adj]);
    }

    @Override public void Swap_Two_Elements(RType A, RType i, RType j) {
        int adjI = ((Integer_Impl.Integer)i).rep.val - lowerBound;
        int adjJ = ((Integer_Impl.Integer)j).rep.val - lowerBound;
        RType[] temp1 = ((Array_Rep)A.getRep()).content;
        swap(temp1[adjI], temp1[adjJ]);
    }

    /*@Override public void Assign_Element(RType A, RType e, RType i) {
        int adjI = ((Integer_Impl.Integer)i).rep.val - lowerBound;
        RType[] temp1 = ((Array_Rep)A.getRep()).content;
        assign(temp1[adjI], e);
    }*/

    @Override public RType getUpper_Bound() {
        return Standard_Integers.INSTANCE.initInteger(upperBound);
    }

    @Override public RType getLower_Bound() {
        return Standard_Integers.INSTANCE.initInteger(lowerBound);
    }

    @Override public RType getT() {
        return type;
    }
}