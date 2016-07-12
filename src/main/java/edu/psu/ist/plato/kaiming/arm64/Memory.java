package edu.psu.ist.plato.kaiming.arm64;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class Memory implements Operand {

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
        mBase = base;
        mOff = null;
    }
    
    public Memory(Register base, long off) {
        mBase = base;
        mOff = new ImmOff(off);
    }
    
    public Memory(Register base, Register off) {
        mBase = base;
        mOff = new RegOff(off);
    }
    
    public Register base() {
        return mBase;
    }
    
    public Offset offset() {
        return mOff;
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
    public boolean isImmediate() {
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
    public Register asRegister() {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printOpMemory(this);
        p.close();
        return baos.toString();
    }
}
