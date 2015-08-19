package edu.psu.ist.plato.kaiming.x86;

public class NopInst extends Instruction {

    protected NopInst(long addr) {
        super(addr, new Opcode("nop"), new Operand[0]);
    }

}
