package edu.psu.ist.plato.kaiming.arm64;

public class BitfieldMoveInst extends Instruction {

    protected BitfieldMoveInst(long addr, Opcode op, Register rd, Register rs,
            Immediate rotate, Immediate idx) {
        super(Kind.BITFIELD_MOVE, addr, op, new Operand[] { rd, rs, rotate, idx });
    }
    
    public Register dest() {
        return operand(0).asRegister();
    }
    
    public boolean isExtension() {
        return this instanceof ExtensionInst;
    }
    
    public boolean isSigned() {
        char first = opcode().rawOpcode().charAt(0);
        return first == 'S';
    }
    
    public Immediate rotate() {
        return operand(2).asImmediate();
    }
    
    public Immediate shift() {
        return operand(3).asImmediate();
    }
}
