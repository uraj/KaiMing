package edu.psu.ist.plato.kaiming.arm64;

import edu.psu.ist.plato.kaiming.Machine;
import edu.psu.ist.plato.kaiming.Machine.Arch;
import edu.psu.ist.plato.kaiming.Machine.MachRegister;
import edu.psu.ist.plato.kaiming.util.Assert;

public class Register extends Operand implements Machine.MachRegister {
    
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
    }
    
    private static final Register x0 = new Register(Id.X0);
    private static final Register x1 = new Register(Id.X1);
    private static final Register x2 = new Register(Id.X2);
    private static final Register x3 = new Register(Id.X3);
    private static final Register x4 = new Register(Id.X4);
    private static final Register x5 = new Register(Id.X5);
    private static final Register x6 = new Register(Id.X6);
    private static final Register x7 = new Register(Id.X7);
    private static final Register x8 = new Register(Id.X8);
    private static final Register x9 = new Register(Id.X9);
    private static final Register x10 = new Register(Id.X10);
    private static final Register x11 = new Register(Id.X11);
    private static final Register x12 = new Register(Id.X12);
    private static final Register x13 = new Register(Id.X13);
    private static final Register x14 = new Register(Id.X14);
    private static final Register x15 = new Register(Id.X15);
    private static final Register x16 = new Register(Id.X16);
    private static final Register x17 = new Register(Id.X17);
    private static final Register x18 = new Register(Id.X18);
    private static final Register x19 = new Register(Id.X19);
    private static final Register x20 = new Register(Id.X20);
    private static final Register x21 = new Register(Id.X21);
    private static final Register x22 = new Register(Id.X22);
    private static final Register x23 = new Register(Id.X23);
    private static final Register x24 = new Register(Id.X24);
    private static final Register x25 = new Register(Id.X25);
    private static final Register x26 = new Register(Id.X26);
    private static final Register x27 = new Register(Id.X27);
    private static final Register x28 = new Register(Id.X28);
    private static final Register x29 = new Register(Id.X29);
    private static final Register x30 = new Register(Id.X30);
    private static final Register xzr = new Register(Id.XZR);
    private static final Register w0 = new Register(Id.W0);
    private static final Register w1 = new Register(Id.W1);
    private static final Register w2 = new Register(Id.W2);
    private static final Register w3 = new Register(Id.W3);
    private static final Register w4 = new Register(Id.W4);
    private static final Register w5 = new Register(Id.W5);
    private static final Register w6 = new Register(Id.W6);
    private static final Register w7 = new Register(Id.W7);
    private static final Register w8 = new Register(Id.W8);
    private static final Register w9 = new Register(Id.W9);
    private static final Register w10 = new Register(Id.W12);
    private static final Register w11 = new Register(Id.W11);
    private static final Register w12 = new Register(Id.W12);
    private static final Register w13 = new Register(Id.W13);
    private static final Register w14 = new Register(Id.W14);
    private static final Register w15 = new Register(Id.W15);
    private static final Register w16 = new Register(Id.W16);
    private static final Register w17 = new Register(Id.W17);
    private static final Register w18 = new Register(Id.W18);
    private static final Register w19 = new Register(Id.W19);
    private static final Register w20 = new Register(Id.W20);
    private static final Register w21 = new Register(Id.W21);
    private static final Register w22 = new Register(Id.W22);
    private static final Register w23 = new Register(Id.W23);
    private static final Register w24 = new Register(Id.W24);
    private static final Register w25 = new Register(Id.W25);
    private static final Register w26 = new Register(Id.W26);
    private static final Register w27 = new Register(Id.W27);
    private static final Register w28 = new Register(Id.W28);
    private static final Register w29 = new Register(Id.W29);
    private static final Register w30 = new Register(Id.W30);
    private static final Register wzr = new Register(Id.WZR);
    private static final Register sp = new Register(Id.SP);
    private static final Register pc = new Register(Id.PC);
    
    public final Id id;
    private Shift mShift;
    
    private Register(Id ID) {
        super(Type.REGISTER);
        id = ID;
        mShift = null;
    }
    
    private Register(Id ID, Shift shift) {
        super(Type.REGISTER);
        id = ID;
    }
    
    public static Register get(Id id, Shift shift) {
        return new Register(id, shift);
    }
    
    public static Register get(Id id) {
        switch (id) {
            case X0: return x0;
            case X1: return x1;
            case X2: return x2;
            case X3: return x3;
            case X4: return x4;
            case X5: return x5;
            case X6: return x6;
            case X7: return x7;
            case X8: return x8;
            case X9: return x9;
            case X10: return x10;
            case X11: return x11;
            case X12: return x12;
            case X13: return x13;
            case X14: return x14;
            case X15: return x15;
            case X16: return x16;
            case X17: return x17;
            case X18: return x18;
            case X19: return x19;
            case X20: return x20;
            case X21: return x21;
            case X22: return x22;
            case X23: return x23;
            case X24: return x24;
            case X25: return x25;
            case X26: return x26;
            case X27: return x27;
            case X28: return x28;
            case X29: return x29;
            case X30: return x30;
            case XZR: return xzr;
            case W0: return w0;
            case W1: return w1;
            case W2: return w2;
            case W3: return w3;
            case W4: return w4;
            case W5: return w5;
            case W6: return w6;
            case W7: return w7;
            case W8: return w8;
            case W9: return w9;
            case W10: return w10;
            case W11: return w11;
            case W12: return w12;
            case W13: return w13;
            case W14: return w14;
            case W15: return w15;
            case W16: return w16;
            case W17: return w17;
            case W18: return w18;
            case W19: return w19;
            case W20: return w20;
            case W21: return w21;
            case W22: return w22;
            case W23: return w23;
            case W24: return w24;
            case W25: return w25;
            case W26: return w26;
            case W27: return w27;
            case W28: return w28;
            case W29: return w29;
            case W30: return w30;
            case WZR: return wzr;
            case SP: return sp;
            case PC: return pc;
            default: throw new IllegalArgumentException("Unkown register");
        }
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

}
