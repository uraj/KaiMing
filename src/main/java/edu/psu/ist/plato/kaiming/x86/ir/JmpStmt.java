package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.JumpInst;

public class JmpStmt extends Stmt {

    private Expr mTarget;
        
    protected JmpStmt(JumpInst inst, Expr target) {
        super(inst);
        mTarget = target;
    }
    
    public Expr getTarget() {
        return mTarget;
    }
}
