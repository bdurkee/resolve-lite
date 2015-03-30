package resolvelite.codegen;

import org.antlr.v4.runtime.misc.NotNull;
import resolvelite.codegen.model.InitCall;

public interface TypeRefInitFactory {

    public InitCall buildTypeInit(@NotNull String initialValue);
}
