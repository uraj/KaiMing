package edu.psu.ist.plato.kaiming.x86;

public class BitTestInst extends Instruction {

    protected BitTestInst(long addr, Opcode op, Operand bt1, Operand bt2) {
        super(Kind.BIT_TEST, addr, op, new Operand[]{bt1, bt2});
    }

    public Operand indexOperand() {
        return operand(0);
    }
    
    public Operand sourceOperand() {
        return operand(1);
    }
}
