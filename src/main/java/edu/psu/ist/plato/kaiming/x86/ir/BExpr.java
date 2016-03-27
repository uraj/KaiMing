package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Opcode;

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
        ASR,
    }
    
    private Op mOperator;
    private Expr mLeft, mRight;
    
    public BExpr(Opcode opcode, Expr left, Expr right) {
        mOperator = opcodeToOp(opcode);
        mLeft = left;
        mRight = right;
    }
    
    public BExpr(Op op, Expr left, Expr right) {
        mOperator = op;
        mLeft = left;
        mRight = right;
    }
    
    // TODO: not implemented
    private Op opcodeToOp(Opcode opcode) {
    	return null;
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
