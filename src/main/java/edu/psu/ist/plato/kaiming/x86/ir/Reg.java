package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Register;

public class Reg extends Lval {
    private Register mReg;

    public Reg(Register reg) {
        mReg = reg;
    }
    
    public Register getRegister() {
        return mReg;
    }
}
