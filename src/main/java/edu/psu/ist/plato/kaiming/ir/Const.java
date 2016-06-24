package edu.psu.ist.plato.kaiming.ir;

public final class Const extends Expr {

	private final long mValue;
	
	private Const(long value) {
		mValue = value;
	}
	
	public static Const get(long value) {
	    return new Const(value);
	}
	
	public long value() {
		return mValue;
	}
	
	public boolean valueEquals(Const that) {
	    return mValue == that.mValue;
	}
	
	@Override
	public int numOfSubExpr() {
		return 0;
	}
	
	@Override
	public Expr subExpr(int index) {
		return null;
	}

    @Override
    public boolean equals(Object that) {
        return this == that;
    }
    
    @Override
    public int hashCode() {
        return (int)mValue;
    }

}
