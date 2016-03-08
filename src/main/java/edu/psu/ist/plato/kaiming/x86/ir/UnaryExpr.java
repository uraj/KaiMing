package edu.psu.ist.plato.kaiming.x86.ir;

public class UnaryExpr extends Expr {
    public enum Op {
        Neg,
    }
    
    private Op mOperator;
    private Expr mSubExpr;
    
    UnaryExpr(Op op, Expr subexpr) {
        mOperator = op;
        mSubExpr = subexpr;
    }
    
    public Op getOperator() {
        return mOperator;
    }
    
    public Expr getSubExpr() {
        return mSubExpr;
    }
    
    @Override
    public Expr getSubExpr(int index) {
        if (index == 0)
            return getSubExpr();
        return null;
    }

    @Override
    public int getNumSubExpr() {
        return 1;
    }
}
