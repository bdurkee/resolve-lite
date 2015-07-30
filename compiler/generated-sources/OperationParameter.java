import java.lang.reflect.*;

public interface OperationParameter extends RType {
    public RType op(RType ... e);
}
