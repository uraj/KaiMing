package edu.psu.ist.plato.kaiming.x86.ir;

public class UExpr extends Expr {
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
}
