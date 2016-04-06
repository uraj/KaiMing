package edu.psu.ist.plato.kaiming.x86;

import java.util.SortedSet;

public class JumpInst extends BranchInst {

    protected JumpInst(long addr, Opcode op, Memory target, boolean isIndirect) {
        super(Kind.JUMP, addr, op, target, isIndirect);
    }
    
    protected JumpInst(long addr, Opcode op, Register target, boolean isIndirect) {
        super(Kind.JUMP, addr, op, target, isIndirect);
    }

    public SortedSet<Flag> dependentFlags() {
        return Flag.getDependentFlagsByCondition("");
    }
}
