package edu.psu.ist.plato.kaiming.arm64;

public class StorePairInst extends LoadStoreInst {

    protected StorePairInst(long addr, Opcode op, Register rd1, Register rd2, Memory mem, AddressingMode mode) {
        super(Kind.STORE_PAIR, addr, op, new Operand[] { rd1, rd2, mem }, mode);
    }
    
    public Register srcLeft() {
        return operand(0).asRegister();
    }

    public Register srcRight() {
        return operand(1).asRegister();
    }
    
    @Override
    public int indexingOperandIndex() {
        return 2;
    }
    
}
