package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.Entry;

public class CallStmt extends DefStmt {

    public CallStmt(Entry inst, Expr target) {
        super(Kind.CALL, inst,
                // TODO: Make this value configurable with a "CallingConvention"
                // structure
                // FIXME: Theoretically, all caller-save registers can be
                // updated by a call. For now, we assume the analyzed code 
                // is "good", meaning the compiler will restore these registers
                // if they are to be used later in the same routine.
                Reg.get(inst.machine().returnRegister()),
                new Expr[] { target });
    }
    
    public Expr target() {
    	return usedExpr(0);
    }
}
