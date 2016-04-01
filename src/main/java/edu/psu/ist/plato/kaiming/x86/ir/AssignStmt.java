package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

public class AssignStmt extends DefStmt {

    private Lval mLval;
    private Expr mExpr;
    
    protected AssignStmt(Instruction inst, Lval lval, Expr expr) {
        super(Kind.ASSIGN, inst);
        mLval = lval;
        mExpr = expr;
    }
    
    @Override
    public Lval getLval() {
        return mLval;
    }
    
    public Expr getExpr() {
        return mExpr;
    }

}
