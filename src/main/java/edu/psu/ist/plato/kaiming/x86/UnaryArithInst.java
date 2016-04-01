package edu.psu.ist.plato.kaiming.x86;

public class UnaryArithInst extends Instruction {

    protected UnaryArithInst(long addr, Opcode opcode, Operand operand) {
        super(Kind.UN_ARITH, addr, opcode, new Operand[] {operand});
    }
    
    public Operand getOperand() { return getOperand(0); }
}
