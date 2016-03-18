package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

public class CmpStmt extends Stmt {

    private Expr mCmp1;
    private Expr mCmp2;
    
    protected CmpStmt(Instruction inst, Expr cmp1, Expr cmp2) {
        super(inst);
        mCmp1 = cmp1;
        mCmp2 = cmp2;
    }
    
    public Expr getComparedFirst() {
        return mCmp1;
    }
    
    public Expr getComparedSecond() {
        return mCmp2;
    }
    
    public Expr[] getCompared() {
        return new Expr[] { mCmp1, mCmp2 };
    }

}
