package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.CallInst;
import edu.psu.ist.plato.kaiming.x86.Register;

public class CallStmt extends DefStmt {

	private Expr mTarget;
	
    public CallStmt(CallInst inst, Expr target) {
        super(Kind.CALL, inst);
        mTarget = target;
    }
    
    public Expr getTarget() {
    	return mTarget;
    }

    @Override
    public Lval getLval() {
        // TODO: Make this value configurable with a "CallingConvention"
        // structure
        // FIXME: Theoretically, all caller-save registers can be
        // updated by a call. For now, we assume the analyzed code 
        // is "good", meaning the compiler will restore these registers
        // if they are to be used later in the same routine.
        return Reg.getReg(Register.getRegister("eax"));
    }

}
