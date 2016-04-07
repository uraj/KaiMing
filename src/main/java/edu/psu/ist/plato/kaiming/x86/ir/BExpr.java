package edu.psu.ist.plato.kaiming.x86.ir;

public class BExpr extends Expr {
   
    public enum Op {
        ADD,
        UADD,
        SUB,
        USUB,
        OR,
        AND,
        XOR,
        MUL,
        DIV,
        UMUL,
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
}
