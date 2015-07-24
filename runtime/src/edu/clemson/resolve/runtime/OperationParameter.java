package edu.clemson.resolve.runtime;

public interface OperationParameter extends RType {
    public RType op(RType ... e);
}