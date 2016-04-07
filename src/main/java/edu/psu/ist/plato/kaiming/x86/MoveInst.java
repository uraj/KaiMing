package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.util.Assert;

public class MoveInst extends Instruction {

    protected MoveInst(long addr, Opcode op, Operand from, Operand to) {
        super(Kind.MOVE, addr, op, new Operand[] { from, to });
        Assert.verify(!(from.isMemory() && to.isMemory()));
    }

    public Operand from() {
        return operand(0);
    }

    public Operand to() {
        return operand(1);
    }

    public boolean isLoad() {
        return from() instanceof Memory;
    }

    public boolean isStore() {
        return to() instanceof Memory;
    }
}
