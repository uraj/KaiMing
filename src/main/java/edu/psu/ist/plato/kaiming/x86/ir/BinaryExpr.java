package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Opcode;

public class BinaryExpr extends Expr {
   
    public enum Op {
        ADD,
        SUB,
        OR,
        AND,
        XOR,
        MUL,
    }
    
    private Op mOperator;
    private Expr mLeft, mRight;
    
    public BinaryExpr(Opcode opcode, Expr left, Expr right) {
        mOperator = opcodeToOp(opcode);
        mLeft = left;
        mRight = right;
    }
    
    public BinaryExpr(Op op, Expr left, Expr right) {
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
