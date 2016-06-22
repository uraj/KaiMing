package edu.psu.ist.plato.kaiming.arm64;

public class MoveInst extends Instruction {

    protected MoveInst(long addr, Opcode op, Register rd, Operand rs) {
        super(Kind.MOVE, addr, op, new Operand[] { rd, rs });
    }
    
    public boolean keep() {
        return opcode().mnemonic() == Opcode.Mnemonic.MOVK;
    }

    public Register dest() {
        return operand(0).asRegister();
    }
    
    public Operand src() {
        return operand(1);
    }
}
