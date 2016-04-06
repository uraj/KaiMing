package edu.psu.ist.plato.kaiming.x86;

import java.util.Set;

public class CondJumpInst extends JumpInst {

    // FIXME: Use bitset to represent EFLAGS
    private final Set<Flag> mCond;
    
    protected CondJumpInst(long addr, Opcode op, Memory target, boolean isIndirect) {
        super(addr, op, target, isIndirect);
        mCond = Flag.getDependentFlagsByCondition(op.rawOpcode().substring(1));
    }
    
    protected CondJumpInst(long addr, Opcode op, Register target, boolean isIndirect) {
        super(addr, op, target, isIndirect);
        mCond = Flag.getDependentFlagsByCondition(op.rawOpcode().substring(1));
    }

    public Set<Flag> dependentFlags() {
        return mCond;
    }
    
    @Override
    public boolean isConditional() {
        return true;
    }
}
