package edu.psu.ist.plato.kaiming.arm64;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import edu.psu.ist.plato.kaiming.Machine.Arch;
import edu.psu.ist.plato.kaiming.MachRegister;
import edu.psu.ist.plato.kaiming.util.Assert;

public final class Register extends MachRegister implements Operand {
    
    public enum Id {
        X0, X1, X2, X3, X4, X5, X6, X7, X8, X9,
        X10, X11, X12, X13, X14, X15, X16, X17, X18, X19,
        X20, X21, X22, X23, X24, X25, X26, X27, X28, X29,
        X30,
        XZR,
        W0, W1, W2, W3, W4, W5, W6, W7, W8, W9,
        W10, W11, W12, W13, W14, W15, W16, W17, W18, W19,
        W20, W21, W22, W23, W24, W25, W26, W27, W28, W29,
        W30,
        WZR,
        SP,
        PC,
    }
    
    public static class Shift {
        public enum Type {
            ASR,
            LSL,
            ROR,
        }
        private Type mSType;
        private int mSh;
        
        public Shift(Type shiftType, int shift) {
            Assert.verify(shift != 0);
            mSh = shift;
            mSType = shiftType;
        }
        
        public Type type() {
            return mSType;
        }
        
        public int value() {
            return mSh;
        }
        
        @Override
        public boolean equals(Object that) {
            if (this == that)
                return true;
            if (that instanceof Shift) {
                Shift t = (Shift)that;
                return mSType == t.mSType && t.mSh == t.mSh;
            }
            return false;
        }
    }
    
    public final Id id;
    private final Shift mShift;
    
    private Register(Id ID, Shift shift) {
        id = ID;
        mShift = shift;
    }
    
    public static Register get(Id id, Shift shift) {
        return new Register(id, shift);
    }
    
    private static Map<Id, Register> sCache = new HashMap<Id, Register>();
    
    public static Register get(Id id) {
        Register ret = sCache.get(id);
        if (ret == null) {
            ret = new Register(id, null);
            sCache.put(id, ret);
        }
        return ret;
    }
    
    public static Register get(String name) {
        return get(Id.valueOf(name.toUpperCase()));
    }

    public boolean isShifted() {
        return mShift != null;
    }
    
    public Shift shift() {
        return mShift;
    }
    
    @Override
    public String name() {
        return id.name();
    }

    @Override
    public Arch arch() {
        return Arch.ARM64;
    }

    @Override
    public int sizeInBits() {
        return id.name().startsWith("W") ? 32 : 64;
    }

    @Override
    public MachRegister containingRegister() {
        String name = name();
        if (name.startsWith("W"))
            return get(name.replace("W", "X"));
        return this;
    }

    @Override
    public Set<MachRegister> subsumedRegisters() {
        Set<MachRegister> ret = new HashSet<MachRegister>();
        String name = name();
        if (name.charAt(0) == 'X') {
            ret.add(get('W' + name.substring(1)));
        }
        return ret;
    }

    @Override
    public boolean isRegister() {
        return true;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isImmeidate() {
        return false;
    }

    @Override
    public Type type() {
        return Type.REGISTER;
    }

    @Override
    public Immediate asImmediate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Memory asMemory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Register asRegister() {
        return this;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that)
            return true;
        if (that instanceof Register) {
            Register reg = (Register)that;
            return id == reg.id && Objects.equals(mShift, reg.shift());
        }
        return false;
    }
    
    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printOpRegister(this);
        p.close();
        return baos.toString();
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

}
