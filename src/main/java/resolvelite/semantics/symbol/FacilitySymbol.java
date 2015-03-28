package resolvelite.semantics.symbol;

import resolvelite.semantics.UnexpectedSymbolException;

public class FacilitySymbol extends BaseSymbol {
    //implements Type {?
    //if we want it to be the rhs of representation type, then yes.

    private final String specName, implName;

    public FacilitySymbol(String name, String specName, String implName) {
        super(name);
        this.specName = specName;
        this.implName = implName;
    }

    public String getSpecName() {
        return specName;
    }

    public String getImplName() {
        return implName;
    }

}
