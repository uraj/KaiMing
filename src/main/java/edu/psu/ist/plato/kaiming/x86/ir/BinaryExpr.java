package edu.psu.ist.plato.kaiming.x86.ir;

public class BinaryExpr extends Expr {
   
    public enum Op {
        Add,
        UAdd,
        Sub,
        Or,
        And,
        Xor,
    }
    
    private Op mOperator;
    private Expr mLeft, mRight;
    
    BinaryExpr(Op op, Expr left, Expr right) {
        mOperator = op;
        mLeft = left;
        mRight = right;
    }
    
    public Expr getLeftSubExpr() {
        return mLeft;
    }
    
    public Expr getRightSubExpr() {
        return mRight;
    }
    
    @Override
    public Expr getSubExpr(int index) {
        if (index == 0)
            return getLeftSubExpr();
        if (index == 1)
            return getRightSubExpr();
        return null;
    }
    
    public Op getOperator() {
        return mOperator;
    }

    @Override
    public int getNumSubExpr() {
        return 2;
    }
}
