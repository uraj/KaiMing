package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

public class AssignStmt extends DefStmt {

    private Lval mLval;
    private Expr mExpr;
    
    public AssignStmt(Instruction inst, Lval lval, Expr expr) {
        super(Kind.ASSIGN, inst, new Expr[] { expr });
        mLval = lval;
        mExpr = expr;
    }
    
    @Override
    public Lval getDefinedLval() {
        return mLval;
    }
    
    public Expr getExpr() {
        return mExpr;
    }

}
