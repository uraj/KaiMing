package edu.psu.ist.plato.kaiming.x86;

public class PushInst extends Instruction {

    protected PushInst(long addr, Opcode op, Operand source) {
        super(addr, op, new Operand[] { source } );
    }

    public Operand getOperand() { return getOperand(0); }
}
