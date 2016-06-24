package edu.psu.ist.plato.kaiming.x86;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Memory implements Operand {

	protected final Register mSeg;
    protected final long mDisp;
    protected final Register mBase;
    protected final Register mOff;
    protected final int mMulti;

    public Memory(Register seg, long disp, Register base, Register off,
            int multiplier) {
        mSeg = seg;
        mDisp = disp;
        mBase = base != null && base.id == Register.Id.EIZ ? null : base;
        mOff = off != null && off.id == Register.Id.EIZ ? null : off;
        mMulti = multiplier;
    }
    
    public Memory(long disp, Register base, Register off,
            int multiplier) {
        mSeg = null;
        mDisp = disp;
        mBase = base != null && base.id == Register.Id.EIZ ? null : base;
        mOff = off != null && off.id == Register.Id.EIZ ? null : off;
        mMulti = multiplier;
    }

    public long displacement() {
        return mDisp;
    }

    public Register baseRegister() {
        return mBase;
    }

    public Register offsetRegister() {
        return mOff;
    }

    public int multiplier() {
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
    
    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printOpMemory(this);
        p.close();
        return baos.toString();
    }

    @Override
    public boolean isRegister() {
        return false;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    @Override
    public boolean isImmeidate() {
        return false;
    }

    @Override
    public Type type() {
        return Type.MEMORY;
    }

    @Override
    public Immediate asImmediate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Memory asMemory() {
        return this;
    }

    @Override
    public Register asRegister() {
        throw new UnsupportedOperationException();
    }

}