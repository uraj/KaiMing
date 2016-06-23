package edu.psu.ist.plato.kaiming.arm64;

public class CompareInst extends Instruction {

    // There are two kinds of compare: subtract and bitwise and
    private boolean mIsTest;
    
    protected CompareInst(long addr, Opcode op, Register cmp1, Operand cmp2, boolean isTest) {
        super(Kind.COMPARE, addr, op, new Operand[] { cmp1, cmp2 });
        mIsTest = isTest;
    }

    public Register comparedLeft() {
        return operand(0).asRegister();
    }
    
    public Register comparedRight() {
        return operand(1).asRegister();
    }
    
    public boolean isTest() {
        return mIsTest;
    }
}
