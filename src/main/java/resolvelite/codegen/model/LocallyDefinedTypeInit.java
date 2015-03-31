package resolvelite.codegen.model;

public class LocallyDefinedTypeInit extends Expr {

    public String name, moduleQualifier;

    public LocallyDefinedTypeInit(String name, String moduleQualifier) {
        this.name = name;
        this.moduleQualifier = moduleQualifier;
    }
}
