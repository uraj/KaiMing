package edu.psu.ist.plato.kaiming.x86;

public class PopInst extends Instruction {

    protected PopInst(long addr, Opcode op, Register target) {
        super(addr, op, new Operand[] { target } );
    }

    public Register getTarget() { return (Register)getOperand(0); }
}
