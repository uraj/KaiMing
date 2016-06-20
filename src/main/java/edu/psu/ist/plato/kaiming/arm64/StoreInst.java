package edu.psu.ist.plato.kaiming.arm64;

public class StoreInst extends Instruction {

    protected StoreInst(long addr, Opcode op, Register rs, Memory mem) {
        super(Kind.STORE, addr, op, new Operand[] { rs, mem });
    }
    
    public Memory src() {
        return operand(0).asMemory();
    }
    
    public Register dest() {
        return operand(1).asRegister();
    }

}
