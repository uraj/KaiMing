package edu.psu.ist.plato.kaiming.arm64;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class Immediate implements Operand {

    private final long mValue;
    
    private Immediate(long value) {
        mValue = value;
    }
    
    // To save memory, we can predefine some commonly used immediate values
    private static final Immediate sZero = new Immediate(0);
    private static final Immediate sPosOne = new Immediate(1);
    private static final Immediate sPosTwo = new Immediate(2);
    private static final Immediate sPosFour = new Immediate(4);
    private static final Immediate sPosEight = new Immediate(8);
    private static final Immediate sNegOne = new Immediate(-1);
    private static final Immediate sNegTwo = new Immediate(-2);
    private static final Immediate sNegFour = new Immediate(-4);
    private static final Immediate sNegEight = new Immediate(-8);
    
    
    public static Immediate get(long value) {
        int nv = (int)value;
        if (nv != value)
            return new Immediate(value);
        switch (nv) {
            case 0: return sZero;
            case 1: return sPosOne;
            case 2: return sPosTwo;
            case 4: return sPosFour;
            case 8: return sPosEight;
            case -1: return sNegOne;
            case -2: return sNegTwo;
            case -4: return sNegFour;
            case -8: return sNegEight;
            default:
                return new Immediate(value);
        }
    };

    public final long value() {
        return mValue;
    }

    @Override
    public boolean isRegister() {
        return false;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isImmeidate() {
        return true;
    }

    @Override
    public Type type() {
        return Type.IMMEDIATE;
    }

    @Override
    public Memory asMemory() {
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
        p.printOpImmediate(this);
        p.close();
        return baos.toString();
    }
}