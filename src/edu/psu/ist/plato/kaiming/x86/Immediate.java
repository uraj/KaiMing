package edu.psu.ist.plato.kaiming.x86;

public class Immediate extends Operand {

	private final long mValue;
	
	private Immediate(long value) {
		super(Type.Immeidate);
		mValue = value;
	}
	
	// To save memory, we can predefine some commonly used immediate values
	private static final Immediate sZero = new Immediate(0);
	private static final Immediate sPosOne = new Immediate(1);
	private static final Immediate sPosTwo = new Immediate(2);
	private static final Immediate sPosFour = new Immediate(4);
	
	public static Immediate getImmediate(long value) {
		if (value == 0)
			return sZero;
		else if (value == 1)
			return sPosOne;
		else if (value == 2)
			return sPosTwo;
		else if (value == 4)
			return sPosFour;
		else
			return new Immediate(value);
	};

	public long getValue() {
		return mValue;
	}
}
