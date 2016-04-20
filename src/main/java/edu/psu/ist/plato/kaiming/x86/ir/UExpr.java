package edu.psu.ist.plato.kaiming.x86.ir;

public final class UExpr extends Expr {
    public enum Op {
        NOT,
        LOW,
        HIGH,
        BSWAP,
    }
    
    private Op mOperator;
    private Expr mSubExpr;
    
    UExpr(Op op, Expr subexpr) {
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
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((mOperator == null) ? 0 : mOperator.hashCode());
        result = prime * result
                + ((mSubExpr == null) ? 0 : mSubExpr.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object that) {
        if (!(that instanceof UExpr)) {
            return false;            
        }
        UExpr t = (UExpr)that;
        return mOperator.equals(t.mOperator) && mSubExpr.equals(t.mSubExpr);
    }
}
