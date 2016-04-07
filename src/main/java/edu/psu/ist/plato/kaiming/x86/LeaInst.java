package edu.psu.ist.plato.kaiming.x86;

public class LeaInst extends Instruction {

    protected LeaInst(long addr, Opcode op, Memory op1, Register op2) {
        super(Kind.LEA, addr, op, new Operand[] {op1, op2});
    }

    public Memory addrExpression() {
        return operand(0).asMemory();
    }
    
    public Register loadedRegister() {
        return operand(1).asRegister();
    }
}
