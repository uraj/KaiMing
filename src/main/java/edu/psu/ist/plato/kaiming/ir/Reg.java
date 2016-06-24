package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.MachRegister;

public final class Reg extends Lval {
    private final MachRegister mReg;
    
    private Reg(MachRegister mreg) {
        mReg = mreg;
    }
    
    public static Reg get(MachRegister mreg) {
        return new Reg(mreg);
    }
    
    @Override
    public boolean equals(Object lv) {
        if (this == lv)
            return true;
        if (lv instanceof Reg) {
            return mReg.equals(((Reg)lv).mReg);
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
        return get(mReg.containingRegister());
    }
    
    public MachRegister machRegister() {
        return mReg;
    }
    
}
