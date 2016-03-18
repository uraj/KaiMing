package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.CallInst;

public class CallStmt extends Stmt {

	private Expr mTarget;
	
    protected CallStmt(CallInst inst, Expr target) {
        super(inst);
        mTarget = target;
    }
    
    public Expr getTarget() {
    	return mTarget;
    }

}
