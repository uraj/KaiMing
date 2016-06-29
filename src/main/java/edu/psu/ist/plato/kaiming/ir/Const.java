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
        if (this == that)
            return true;
        return that instanceof Const && ((Const)that).mValue == mValue;
    }
    
    @Override
    public int hashCode() {
        return (int)mValue;
    }

    @Override
    public Expr substitute(Expr o, Expr n) {
        return this.equals(o) ? n : this;
    }

    @Override
    public boolean contains(Expr o) {
        return this.equals(o);
    }

}
