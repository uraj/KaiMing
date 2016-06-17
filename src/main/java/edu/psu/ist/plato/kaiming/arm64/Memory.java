package edu.psu.ist.plato.kaiming.arm64;

public class Memory extends Operand {

    public abstract class Offset {
        public abstract boolean isImmediateOffset();
    }

    public class ImmOff extends Offset {
        public final int value;
        public ImmOff(int off) {
            value = off;
        }
        public boolean isImmediateOffset() {
            return true;
        }
    }
    
    public class ShiftedOff extends Offset {
        public final ShiftedRegister value;
        public ShiftedOff(ShiftedRegister off) {
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
        mOff = null;
    }
    
    public Memory(Register base, int off) {
        super(Type.MEMORY);
        mBase = base;
        mOff = new ImmOff(off);
    }
    
    public Memory(Register base, ShiftedRegister off) {
        super(Type.MEMORY);
        mBase = base;
        mOff = new ShiftedOff(off);
    }
    
    public Register base() {
        return mBase;
    }
    
    public Offset offset() {
        return mOff;
    }
    
}
