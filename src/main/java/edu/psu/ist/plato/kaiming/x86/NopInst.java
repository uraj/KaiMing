package edu.psu.ist.plato.kaiming.x86;

public class NopInst extends Instruction {

    protected NopInst(long addr) {
        super(Kind.NOP, addr, new Opcode("nop"), new Operand[0]);
    }

}
