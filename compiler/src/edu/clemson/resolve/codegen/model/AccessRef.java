package edu.clemson.resolve.codegen.model;

public class AccessRef extends Expr {
    @ModelElement public final Expr left, right;

    public AccessRef(Expr l, Expr r) {
        this.left = l;
        this.right = r;
    }

    public static class LeafAccessRefLeft extends Expr {
        @ModelElement public Expr name;
        public String type;

        public LeafAccessRefLeft(String type, Expr name) {
            this.name = name;
            this.type = type;
        }
    }

    public static class LeafAccessRefRight extends Expr {
        @ModelElement public Expr name;
        public LeafAccessRefRight(Expr name) {
            this.name = name;
        }
    }
}
