package edu.psu.ist.plato.kaiming.arm64;

public class StorePairInst extends Instruction {

    protected StorePairInst(long addr, Opcode op, Register rd1, Register rd2, Memory mem) {
        super(Kind.STORE_PAIR, addr, op, new Operand[] { rd1, rd2, mem });
    }
    
    public Register srcLeft() {
        return operand(0).asRegister();
    }

    public Register srcRight() {
        return operand(1).asRegister();
    }
    
    public Memory dest() {
        return operand(2).asMemory();
    }
    
}
