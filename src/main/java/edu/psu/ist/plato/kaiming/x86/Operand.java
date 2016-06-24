package edu.psu.ist.plato.kaiming.x86;

public interface Operand {
	
	public enum Type {
		REGISTER,
		MEMORY,
		IMMEDIATE,
	}
	
	public boolean isRegister();
	
	public boolean isMemory();

	public boolean isImmeidate();
	
	public Type type();
	
	public Immediate asImmediate();
	
	public Memory asMemory();
	
	public Register asRegister();
	
}
