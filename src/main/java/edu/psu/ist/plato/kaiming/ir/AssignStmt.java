package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

public class AssignStmt extends DefStmt {

    
    private Expr mExpr;
    
    public AssignStmt(Instruction inst, Lval lval, Expr expr) {
        super(Kind.ASSIGN, inst, lval, new Expr[] { expr });
        mExpr = expr;
    }
    
    public Expr usedRval() {
        return mExpr;
    }

}
