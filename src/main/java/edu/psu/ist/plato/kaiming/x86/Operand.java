package edu.psu.ist.plato.kaiming.x86;

abstract public class Operand {
	
	public enum Type {
		Register,
		Memory,
		Immediate,
	}
	
	private final Type mType;
	
	public Operand(Type type) {
		mType = type;
	}
	
	public final boolean isRegister() { 
	    return mType == Type.Register;
	}
	
	public final boolean isMemory() {
	    return mType == Type.Memory;
	}

	public final boolean isImmeidate() { 
	    return mType == Type.Immediate; 
	}
	
	public final Type getType() { 
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
