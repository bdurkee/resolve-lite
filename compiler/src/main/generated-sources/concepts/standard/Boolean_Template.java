package concepts.standard;

import org.resolvelite.runtime.RType;
import org.resolvelite.runtime.ResolveInterface;

public interface Boolean_Template extends ResolveInterface {

    interface Boolean extends RType {}

    public RType createBoolean();

    public RType createBoolean(boolean b);

    public RType True();

    public RType False();

    public RType And(RType a, RType b);

}
