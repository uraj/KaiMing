package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.Machine;
import edu.psu.ist.plato.kaiming.Machine.MachRegister;

public final class Reg extends Lval {
    private final Machine.MachRegister mReg;
    
    private Reg(MachRegister mreg) {
        mReg = mreg;
    }
    
    public static Reg getReg(MachRegister mreg) {
        return new Reg(mreg);
    }
    
    public boolean equalsTo(Reg reg) {
        return mReg.arch() == reg.mReg.arch() && 
                mReg.name().equals(reg.mReg.name());
    }
    
    @Override
    public boolean equals(Object lv) {
        if (lv instanceof Reg) {
            return equalsTo((Reg)lv);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mReg.hashCode();
    }

    @Override
    public int sizeInBits() {
        return mReg.sizeInBits();
    }
    
    public Reg containingReg() {
        return getReg(mReg.containingRegister());
    }
    
    public Machine.MachRegister machRegister() {
        return mReg;
    }
    
}
