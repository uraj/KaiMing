package edu.psu.ist.plato.kaiming.arm64;

public class BitfieldMoveInst extends Instruction {
    
    public enum Extension {
        SIGNED,
        UNSIGNED,
        NIL,
    }

    private Extension mExt;
    
    protected BitfieldMoveInst(long addr, Opcode op, Register rd, Register rs,
            Immediate rotate, Immediate idx) {
        super(Kind.BITFIELD_MOVE, addr, op, new Operand[] { rd, rs, rotate, idx });
        char first = opcode().rawOpcode().charAt(0);
        if (first == 'S')
            mExt = Extension.SIGNED;
        else if (first == 'U')
            mExt = Extension.UNSIGNED;
        else
            mExt = Extension.NIL;
    }
    
    public Register dest() {
        return operand(0).asRegister();
    }
    
    public Extension extension() {
        return mExt;
    }
    
    public Immediate rotate() {
        return operand(2).asImmediate();
    }
    
    public Immediate shift() {
        return operand(3).asImmediate();
    }
}
