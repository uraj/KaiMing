package edu.psu.ist.plato.kaiming.arm64;

public interface Operand {
    
    public enum Type {
        IMMEDIATE,
        REGISTER,
        MEMORY,
    }
    
    public boolean isRegister();
    
    public boolean isMemory();

    public boolean isImmeidate();
    
    public Type type();
    
    public Immediate asImmediate();
    
    public Memory asMemory();
    
    public Register asRegister();
    
}
