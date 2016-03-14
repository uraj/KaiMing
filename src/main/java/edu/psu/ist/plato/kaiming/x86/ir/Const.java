package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Immediate;

public class Const extends Expr {

	private final long mValue;
	
	private Const(long value) {
		mValue = value;
	}
	
	private static final Const constn1 = new Const(-1);
	private static final Const constn2 = new Const(-2);
	private static final Const constn4 = new Const(-4);
	private static final Const constn8 = new Const(-8);
	private static final Const constn16 = new Const(-16);
	private static final Const constn32 = new Const(-32);
	private static final Const const0 = new Const(0);
	private static final Const const1 = new Const(1);
	private static final Const const2 = new Const(2);
	private static final Const const4 = new Const(4);
	private static final Const const8 = new Const(8);
	private static final Const const16 = new Const(16);
	private static final Const const32 = new Const(32);
	
	public static Const getConstant(long value) {
		if (-32 <= value && value <= 32) {
			switch ((int)value) {
				case -1: return constn1;
				case -2: return constn2;
				case -4: return constn4;
				case -8: return constn8;
				case -16: return constn16;
				case -32: return constn32;
				case 0: return const0;
				case 1: return const1;
				case 2: return const2;
				case 4: return const4;
				case 8: return const8;
				case 16: return const16;
				case 32: return const32;
				default: return new Const(value);
			}
		} else {
			return new Const(value);
		}
	}
	
	public static Const getConstant(Immediate imm) {
		return getConstant(imm.getValue());
	}
	
	public long getValue() {
		return mValue;
	}
	
	@Override
	public int getNumSubExpr() {
		return 0;
	}
	
	@Override
	public Expr getSubExpr(int index) {
		return null;
	}

}
