package edu.psu.ist.plato.kaiming.x86;

import java.util.SortedSet;

public class CondJumpInst extends JumpInst {

    // FIXME: Use bitset to represent EFLAGS
    private final SortedSet<Flag> mCond;
    
    protected CondJumpInst(long addr, Opcode op, Memory target, boolean isIndirect) {
        super(addr, op, target, isIndirect);
        mCond = Flag.getDependentFlagsByCondition(op.rawOpcode().substring(1));
    }
    
    protected CondJumpInst(long addr, Opcode op, Register target, boolean isIndirect) {
        super(addr, op, target, isIndirect);
        mCond = Flag.getDependentFlagsByCondition(op.rawOpcode().substring(1));
    }

    @Override
    public SortedSet<Flag> dependentFlags() {
        return mCond;
    }
    
    @Override
    public boolean isConditional() {
        return true;
    }
}
