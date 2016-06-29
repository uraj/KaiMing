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
    
    public default Immediate asImmediate() {
        return (Immediate)this;
    }
    
    public default Memory asMemory() {
        return (Memory)this;
    }
    
    public default Register asRegister() {
        return (Register)this;
    }
    
}
