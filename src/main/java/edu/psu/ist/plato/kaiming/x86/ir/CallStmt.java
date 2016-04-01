package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.CallInst;

public class CallStmt extends Stmt {

	private Expr mTarget;
	
    public CallStmt(CallInst inst, Expr target) {
        super(Kind.CALL, inst);
        mTarget = target;
    }
    
    public Expr getTarget() {
    	return mTarget;
    }

}
