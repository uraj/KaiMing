package edu.psu.ist.plato.kaiming.x86;

public class ReturnInst extends Instruction{

    private final static Operand[] sDummyOperands = new Operand[0];
    
    protected ReturnInst(long addr, Opcode op) {
        super(Kind.RETURN, addr, op, sDummyOperands);
    }

}
