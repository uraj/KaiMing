package edu.psu.ist.plato.kaiming.ir;

public final class BExpr extends Expr {
   
    public enum Op {
        ADD,
        SUB,
        OR,
        AND,
        XOR,
        MUL,
        DIV,
        CONCAT,
        SHL,
        SHR,
        SAR,
    }
    
    private Op mOperator;
    private Expr mLeft, mRight;
    
    public BExpr(Op op, Expr left, Expr right) {
        mOperator = op;
        mLeft = left;
        mRight = right;
    }
    
    public Expr leftSubExpr() {
        return mLeft;
    }
    
    public Expr rightSubExpr() {
        return mRight;
    }
    
    @Override
    public Expr subExpr(int index) {
        if (index == 0)
            return leftSubExpr();
        if (index == 1)
            return rightSubExpr();
        throw new IllegalArgumentException("Sub expression index out of bound");
    }
    
    public Op operator() {
        return mOperator;
    }

    @Override
    public int numOfSubExpr() {
        return 2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mLeft == null) ? 0 : mLeft.hashCode());
        result = prime * result
                + ((mOperator == null) ? 0 : mOperator.hashCode());
        result = prime * result + ((mRight == null) ? 0 : mRight.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object that) {
        if (!(that instanceof BExpr)) {
            return false;
        }
        BExpr t = (BExpr)that;
        return mOperator.equals(t.mOperator) && 
                mLeft.equals(t.mLeft) && mRight.equals(t.mRight);
    }
}
