package edu.psu.ist.plato.kaiming.x86;

public class PopInst extends Instruction {

    protected PopInst(long addr, Opcode op, Register target) {
        super(Kind.POP, addr, op, new Operand[] { target } );
    }

    public Register popTarget() { return operand(0).asRegister(); }
    
    public int sizeInBits() {
        return popTarget().getSizeInBits();
    }
}
