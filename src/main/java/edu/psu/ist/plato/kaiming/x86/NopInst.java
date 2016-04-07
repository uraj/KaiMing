package edu.psu.ist.plato.kaiming.x86;

public class NopInst extends Instruction {

    private static Opcode sNopOp = new Opcode("nop");
    private static Operand[] sEmptyOperands = new Operand[] {};
    
    protected NopInst(long addr) {
        super(Kind.NOP, addr, sNopOp, sEmptyOperands);
    }

}
