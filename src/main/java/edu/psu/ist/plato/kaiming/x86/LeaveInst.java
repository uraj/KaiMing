package edu.psu.ist.plato.kaiming.x86;

public class LeaveInst extends Instruction {

    private static Operand[] sEmpty = new Operand[0];
    private static Opcode sOp = new Opcode("leave");
    
    protected LeaveInst(long addr) {
        super(Kind.LEAVE, addr, sOp, sEmpty);
    }

}
