/** Generated from Boolean_Template.resolve by RESOLVE version 0.0.1. This file should not be modified. */
package concepts.boolean_template;

import java.lang.reflect.*;
import edu.clemson.resolve.runtime.*;

public interface Boolean_Template {
    interface Boolean extends RType { }
    public RType initBoolean(boolean... e);
    public RType True();
    public RType False();
    public RType And(RType a, RType b);
    public RType Or(RType a, RType b);
    public RType Not(RType a);
    public RType Are_Equal(RType a, RType b);
    public RType Are_Not_Equal(RType a, RType b);
}