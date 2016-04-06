package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.JumpInst;

public class JmpStmt extends Stmt {

    private Expr mTarget;
    
    public JmpStmt(JumpInst inst, Expr target) {
        super(Kind.JMP, inst, new Expr[] {target});
        mTarget = target;
    }
    
    public Expr target() {
        return mTarget;
    }
    
    public boolean isConditional() {
    	return ((JumpInst)mInst).isConditional();
    }
}
