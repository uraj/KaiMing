package edu.psu.ist.plato.kaiming.ir;

public final class UExpr extends Expr {
    public enum Op {
        NOT,
        LOW,
        HIGH,
        BSWAP,
        // operators for extracting flags 
        CARRY,
        ZERO,
        NEGATIVE,
        OVERFLOW,
    }
    
    private final Op mOperator;
    private final Expr mSubExpr;
    
    protected UExpr(Op op, Expr subexpr) {
        mOperator = op;
        mSubExpr = subexpr;
    }
    
    public Op operator() {
        return mOperator;
    }
    
    public Expr subExpr() {
        return mSubExpr;
    }
    
    @Override
    public Expr subExpr(int index) {
        if (index == 0)
            return subExpr();
        return null;
    }

    @Override
    public int numOfSubExpr() {
        return 1;
    }

    @Override
    public int hashCode() {
        return mOperator.hashCode() * 31 + mSubExpr.hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (this == that)
            return true;
        if (!(that instanceof UExpr)) {
            return false;            
        }
        UExpr t = (UExpr)that;
        return mOperator.equals(t.mOperator) && mSubExpr.equals(t.mSubExpr);
    }
    
    @Override
    public Expr substitute(Expr o, Expr n) {
        if (this.equals(o))
            return n;
        Expr sub = mSubExpr.substitute(o, n);
        if (!this.equals(sub)) {
            return new UExpr(mOperator, sub);
        }
        return this;
    }

    @Override
    public boolean contains(Expr o) {
        if (this.equals(o))
            return true;
        return mSubExpr.contains(o);
    }
}
