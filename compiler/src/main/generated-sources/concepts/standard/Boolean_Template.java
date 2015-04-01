package concepts.standard;

import org.resolvelite.runtime.RType;
import org.resolvelite.runtime.ResolveInterface;

public interface Boolean_Template extends ResolveInterface {

    interface Boolean extends RType {}

    public RType True();

    public RType False();

    public RType And(RType a, RType b);

}
