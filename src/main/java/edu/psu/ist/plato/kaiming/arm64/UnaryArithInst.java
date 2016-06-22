package edu.psu.ist.plato.kaiming.arm64;

public class UnaryArithInst extends Instruction {

    protected UnaryArithInst(long addr, Opcode op, Register rd, Operand operand) {
        super(Kind.UN_ARITH, addr, op, new Operand[] { rd, operand });
    }

    public Register dest() {
        return operand(0).asRegister();
    }
    
    public Operand src() {
        return operand(1);
    }
}
