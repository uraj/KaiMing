package edu.psu.ist.plato.kaiming.x86;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CallInst extends BranchInst {

    // FIXME: this is actually OS dependent!
    private static final Set<Register.Id> sCallerSavedRegs;
    
    static {
        Set<Register.Id> csr = new HashSet<Register.Id>();
        csr.add(Register.Id.EAX);
        csr.add(Register.Id.ECX);
        csr.add(Register.Id.EDX);
        sCallerSavedRegs = Collections.unmodifiableSet(csr);
    }
    
    
    private Function mCalled;
    protected CallInst(long addr, Opcode op, Operand operand, boolean isIndirect) {
        super(addr, op, operand, isIndirect);
        mCalled = null;
    }

    public void setCalledFunction(Function f) {
        mCalled = f;
    }
    
    public Function getCalledFunction() {
        return mCalled;
    }
    
    public static final Set<Register.Id> getCallerSavedRegister() {
        return sCallerSavedRegs;
    }
}
