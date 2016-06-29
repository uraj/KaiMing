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
        // Shift
        SHL, SHR, SAR, ROR,
        // Extension
        SEXT, UEXT,
    }
    
    private final Op mOperator;
    private final Expr mLeft, mRight;
    
    protected BExpr(Op op, Expr left, Expr right) {
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
        int result = mLeft.hashCode();
        result = 31 * result + mOperator.hashCode();
        result = 31 * result + mRight.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that)
            return true;
        if (!(that instanceof BExpr)) {
            return false;
        }
        BExpr t = (BExpr)that;
        return mOperator.equals(t.mOperator) && 
                mLeft.equals(t.mLeft) && mRight.equals(t.mRight);
    }
    
    @Override
    public Expr substitute(Expr o, Expr n) {
        if (this.equals(o))
            return n;
        Expr left = mLeft.substitute(o, n);
        Expr right = mRight.substitute(o, n);
        if ((!left.equals(mLeft)) || (!right.equals(mRight))) {
            return new BExpr(mOperator, left, right);
        } else
            return this;
    }

    @Override
    public boolean contains(Expr o) {
        if (this.equals(o))
            return true;
        return mLeft.contains(o) || mRight.contains(o);
    }
}
