package edu.psu.ist.plato.kaiming.ir;

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
    public Lval definedLval() {
        return mLval;
    }
    
    public Expr usedRval() {
        return mExpr;
    }

}
