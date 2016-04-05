package edu.psu.ist.plato.kaiming.x86;

public class Memory extends Operand {

	protected final Register mSeg;
    protected final long mDisp;
    protected final Register mBase;
    protected final Register mOff;
    protected final int mMulti;

    public Memory(Register seg, long disp, Register base, Register off,
            int multiplier) {
        super(Type.Memory);
        mSeg = seg;
        mDisp = disp;
        mBase = base != null && base.id == Register.Id.EIZ ? null : base;
        mOff = off != null && off.id == Register.Id.EIZ ? null : off;
        mMulti = multiplier;
    }
    
    public Memory(long disp, Register base, Register off,
            int multiplier) {
        super(Type.Memory);
        mSeg = null;
        mDisp = disp;
        mBase = base != null && base.id == Register.Id.EIZ ? null : base;
        mOff = off != null && off.id == Register.Id.EIZ ? null : off;
        mMulti = multiplier;
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
    
    public boolean isRelocation() {
        return false;
    }
    
    public Relocation asRelocation() {
        return (Relocation)this;
    }
    
}