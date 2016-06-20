package edu.psu.ist.plato.kaiming.arm64;

public class CompareInst extends Instruction {

    protected CompareInst(long addr, Opcode op, Register rd, Register r2) {
        super(Kind.COMPARE, addr, op, new Operand[] { rd, r2 });
    }

    
}
