package org.rsrg.semantics.symbol;

import org.rsrg.semantics.MTType;
import org.rsrg.semantics.programtype.PTType;

public interface ModuleArgSymbol {

    public MTType getMathType();

    public PTType getProgramType();
}