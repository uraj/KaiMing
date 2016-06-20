package edu.psu.ist.plato.kaiming.arm64;

public class NopInst extends Instruction {
    
    private static Opcode sOp = new Opcode("NOP");

    protected NopInst(long addr) {
        super(Kind.NOP, addr, sOp, new Operand[] {});
    }

}
