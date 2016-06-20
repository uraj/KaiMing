package edu.psu.ist.plato.kaiming.arm64;

public class Memory extends Operand {

    public abstract class Offset {
        public abstract boolean isImmediateOffset();
        public ImmOff asImmOff() {
            return (ImmOff)this;
        }
        public RegOff asRegOff() {
            return (RegOff)this;
        }
    }

    public class ImmOff extends Offset {
        public final long value;
        public ImmOff(long off) {
            value = off;
        }
        public boolean isImmediateOffset() {
            return true;
        }
    }
    
    public class RegOff extends Offset {
        public final Register value;
        public RegOff(Register off) {
            value = off;
        }
        public boolean isImmediateOffset() {
            return false;
        }
    }

    private Register mBase;
    private Offset mOff;
    
    public Memory(Register base) {
        super(Type.MEMORY);
        mBase = base;
        mOff = null;
    }
    
    public Memory(Register base, long off) {
        super(Type.MEMORY);
        mBase = base;
        mOff = new ImmOff(off);
    }
    
    public Memory(Register base, Register off) {
        super(Type.MEMORY);
        mBase = base;
        mOff = new RegOff(off);
    }
    
    public Register base() {
        return mBase;
    }
    
    public Offset offset() {
        return mOff;
    }
    
}
