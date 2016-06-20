package edu.psu.ist.plato.kaiming.arm64;

public class LoadInst extends Instruction {

    protected LoadInst(long addr, Opcode op, Register rd, Memory mem) {
        super(Kind.LOAD, addr, op, new Operand[] { rd, mem });
    }

    public Register dest() {
        return operand(0).asRegister();
    }
    
    public Memory src() {
        return operand(1).asMemory();
    }
}
