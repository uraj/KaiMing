package edu.psu.ist.plato.kaiming.arm64;

public class ShiftedRegister extends Operand {
    public enum ShiftType {
        ASR,
        LSL,
        ROR,
    }
    
    private Register mReg;
    private ShiftType mSType;
    private int mSh;
    
    public ShiftedRegister(Register reg, ShiftType shiftType, int shift) {
        super(Type.SHIFTED_REGISTER);
        mReg = reg;
        mSh = shift;
        if (mSh > 0)
            mSType = shiftType;
        else
            mSType = null;
    }
    
    public ShiftedRegister(Register reg) {
        super(Type.SHIFTED_REGISTER);
        mReg = reg;
        mSh = 0;
        mSType = null;
    }
    
    public Register register() {
        return mReg;
    }
    
    public ShiftType shiftType() {
        return mSType;
    }
    
    public int shift() {
        return mSh;
    }
    
    public boolean isShifted() {
        return mSType != null;
    }
    
}
