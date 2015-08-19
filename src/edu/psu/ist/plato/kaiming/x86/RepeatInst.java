package edu.psu.ist.plato.kaiming.x86;

import java.util.Set;

public class RepeatInst extends Instruction {
    
    private final Instruction mSubInst;
    private final Set<Flag> mCond;
    protected RepeatInst(long addr, Opcode op, Operand[] operands) {
        super(addr, op, new Operand[]{});
        String raw = mOpcode.getRawOpcode();
        String[] raws = raw.split("\\s+");
        mOpcode.setRawOpcode(raws[0]);
        mCond = Flag.getDependentFlagsByCondition(raws[0].substring(3));
        mSubInst = createInstruction(addr, new Opcode(raws[1]), operands, false);
    }

    public Instruction getSubInst() {
        return mSubInst;
    }
    
    public Set<Flag> getDependentFlags() {
        return mCond;
    }
}
