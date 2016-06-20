package edu.psu.ist.plato.kaiming.arm64;

public abstract class Operand {
    
    public enum Type {
        IMMEDIATE,
        REGISTER,
        MEMORY,
    }
    
    private final Type mType;
    
    protected Operand(Type type) {
        mType = type;
    }
    
    public final boolean isRegister() { 
        return mType == Type.REGISTER;
    }
    
    public final boolean isMemory() {
        return mType == Type.MEMORY;
    }

    public final boolean isImmeidate() { 
        return mType == Type.IMMEDIATE;
    }
    
    public final Type type() { 
        return mType;
    }
    
    public final Immediate asImmediate() {
        return (Immediate)this;
    }
    
    public final Memory asMemory() {
        return (Memory)this;
    }
    
    public final Register asRegister() {
        return (Register)this;
    }
    
}
