package edu.psu.ist.plato.kaiming.x86;

public class MoveInst extends Instruction {

    protected MoveInst(long addr, Opcode op, Operand from, Operand to) {
        super(addr, op, new Operand[] { from, to });
    }

    public Operand getFrom() {
        return getOperand(0);
    }

    public Operand getTo() {
        return getOperand(1);
    }

    public boolean isLoad() {
        return getFrom() instanceof Memory;
    }

    public boolean isStore() {
        return getTo() instanceof Memory;
    }
}
