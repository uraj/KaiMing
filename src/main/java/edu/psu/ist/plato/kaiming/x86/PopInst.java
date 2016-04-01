package edu.psu.ist.plato.kaiming.x86;

public class PopInst extends Instruction {

    protected PopInst(long addr, Opcode op, Register target) {
        super(Kind.POP, addr, op, new Operand[] { target } );
    }

    public Register getTarget() { return (Register)getOperand(0); }
    
    public int getOperandSizeInBytes() {
        return getTarget().getSizeInBits() / 8;
    }
}
