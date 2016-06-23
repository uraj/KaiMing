package edu.psu.ist.plato.kaiming.arm64;

public class StoreInst extends LoadStoreInst {

    protected StoreInst(long addr, Opcode op, Register rs, Memory mem, AddressingMode mode) {
        super(Kind.STORE, addr, op, new Operand[] { rs, mem }, mode);
    }
    
    public Register src() {
        return operand(0).asRegister();
    }
    
    public Memory dest() {
        return operand(1).asMemory();
    }

    @Override
    public int indexingOperand() {
        return 1;
    }

}
