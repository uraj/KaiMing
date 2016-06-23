package edu.psu.ist.plato.kaiming.arm64;

public class LoadInst extends LoadStoreInst {

    protected LoadInst(long addr, Opcode op, Register rd, Memory mem, AddressingMode mode) {
        super(Kind.LOAD, addr, op, new Operand[] { rd, mem }, mode);
    }

    public Register dest() {
        return operand(0).asRegister();
    }
    
    @Override
    public int indexingOperandIndex() {
        return 1;
    }
}
