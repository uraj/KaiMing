package edu.psu.ist.plato.kaiming.x86;

import java.util.Set;

public class BinaryArithInst extends Instruction {

    private final Set<Flag> mModifiedFlags;
    
    protected BinaryArithInst(long addr, Opcode op, Operand operand1,
            Operand operand2) {
        super(addr, op, new Operand[] { operand1, operand2 });
        mModifiedFlags = Flag.getModifiedFlagsByOpcode(op.getOpcodeClass());
    }

    public Operand getSrc() {
        return getOperand(0);
    }

    public Operand getDest() {
        return getOperand(1);
    }
    
    public Set<Flag> getModifiedFlags() {
        return mModifiedFlags;
    }
}
