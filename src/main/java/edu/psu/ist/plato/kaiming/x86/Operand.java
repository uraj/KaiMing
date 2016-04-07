package edu.psu.ist.plato.kaiming.x86;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

abstract public class Operand {
	
	public enum Type {
		REGISTER,
		MEMORY,
		IMMEDIATE,
	}
	
	private final Type mType;
	
	public Operand(Type type) {
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
	
	@Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printOperand(this);
        p.close();
        return baos.toString();
    }
}
