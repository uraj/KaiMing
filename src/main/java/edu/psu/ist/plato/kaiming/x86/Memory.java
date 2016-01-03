package edu.psu.ist.plato.kaiming.x86;

public class Memory extends Operand {

    protected final long mDisp;
    protected final Register mBase;
    protected final Register mOff;
    protected final int mMulti;

    public Memory(long disp, Register base, Register off,
            int multi) {
        super(Type.Memory);
        mDisp = disp;
        mBase = base != null && base.id == Register.Id.EIZ ? null : base;
        mOff = off != null && off.id == Register.Id.EIZ ? null : off;
        mMulti = multi;
    }

    public long getDisplacement() {
        return mDisp;
    }

    public Register getBaseRegister() {
        return mBase;
    }

    public Register getOffsetRegister() {
        return mOff;
    }

    public int getMultiplier() {
        return mMulti;
    }

    public boolean isConcrete() {
        return mBase == null && mOff == null;
    }
}