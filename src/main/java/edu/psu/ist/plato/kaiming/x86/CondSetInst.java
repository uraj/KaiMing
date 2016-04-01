package edu.psu.ist.plato.kaiming.x86;

import java.util.Set;

public class CondSetInst extends Instruction {
    
    private final Set<Flag> mCond;
    
    protected CondSetInst(long addr, Opcode op, Operand operand) {
        super(Kind.COND_SET, addr, op, new Operand[] {operand});
        mCond = Flag.getDependentFlagsByCondition(op.getRawOpcode().substring(3));
    }

    public Set<Flag> getDependentFlags() {
        return mCond;
    }
    
    public Operand getDest() {
        return getOperand(0);
    }
    
    @Override
    public boolean isConditional() {
        return true;
    }
}
