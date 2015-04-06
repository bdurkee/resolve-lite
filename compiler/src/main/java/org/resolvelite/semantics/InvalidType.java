package org.resolvelite.semantics;

public class InvalidType implements Type {
    public static final InvalidType INSTANCE = new InvalidType();

    private InvalidType() {}

    public String getRootModuleID() {
        return "Invalid";
    }

    @Override public String getName() {
        return "Invalid";
    }
}
