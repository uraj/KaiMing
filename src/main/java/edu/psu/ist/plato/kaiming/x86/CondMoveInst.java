package edu.psu.ist.plato.kaiming.x86;

import java.util.Set;

public class CondMoveInst extends MoveInst {

    private final Set<Flag> mCond;

    protected CondMoveInst(long addr, Opcode op, Operand from, Operand to) {
        super(addr, op, from, to);
        mCond = Flag.getDependentFlagsByCondition(op.rawOpcode()
                .substring(4));
    }

    public Set<Flag> dependentFlags() {
        return mCond;
    }
    
    @Override
    public boolean isConditional() {
        return true;
    }
}
