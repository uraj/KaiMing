package edu.psu.ist.plato.kaiming.arm64;

public class LoadPairInst extends LoadStoreInst {

    protected LoadPairInst(long addr, Opcode op, Register rd1, Register rd2, Memory mem, AddressingMode mode) {
        super(Kind.LOAD_PAIR, addr, op, new Operand[] { rd1, rd2, mem }, mode);
    }
    
    public Register destLeft() {
        return operand(0).asRegister();
    }
    
    public Register destRight() {
        return operand(1).asRegister();
    }

    @Override
    public int indexingOperandIndex() {
        return 2;
    }
}
