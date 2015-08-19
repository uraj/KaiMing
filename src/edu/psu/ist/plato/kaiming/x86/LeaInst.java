package edu.psu.ist.plato.kaiming.x86;

public class LeaInst extends Instruction {

    protected LeaInst(long addr, Opcode op, Memory op1, Register op2) {
        super(addr, op, new Operand[] {op1, op2});
    }

    public Memory getExpression() {
        return getOperand(0).asMemory();
    }
    
    public Register getResult() {
        return getOperand(1).asRegister();
    }
}
