package resolvelite.semantics;

public class InvalidType implements Type {
    public static final InvalidType INSTANCE = new InvalidType();

    private InvalidType() {}

    @Override public String getName() {
        return "INVALID";
    }
}
