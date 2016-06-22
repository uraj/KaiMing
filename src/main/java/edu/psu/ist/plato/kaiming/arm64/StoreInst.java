package edu.psu.ist.plato.kaiming.arm64;

public class StoreInst extends Instruction {

    protected StoreInst(long addr, Opcode op, Register rs, Memory mem) {
        super(Kind.STORE, addr, op, new Operand[] { rs, mem });
    }
    
    public Register src() {
        return operand(0).asRegister();
    }
    
    public Memory dest() {
        return operand(1).asMemory();
    }

}
