package edu.psu.ist.plato.kaiming.x86;

public class MoveInst extends Instruction {

    protected MoveInst(long addr, Opcode op, Operand from, Operand to) {
        super(Kind.MOVE, addr, op, new Operand[] { from, to });
    }

    public Operand from() {
        return getOperand(0);
    }

    public Operand to() {
        return getOperand(1);
    }

    public boolean isLoad() {
        return from() instanceof Memory;
    }

    public boolean isStore() {
        return to() instanceof Memory;
    }
}
