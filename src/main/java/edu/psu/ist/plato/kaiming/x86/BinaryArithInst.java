package edu.psu.ist.plato.kaiming.x86;

import java.util.Set;

public class BinaryArithInst extends Instruction {

    private final Set<Flag> mModifiedFlags;
    
    protected BinaryArithInst(long addr, Opcode op, Operand operand1,
            Operand operand2) {
        super(Kind.BIN_ARITH, addr, op, new Operand[] { operand1, operand2 });
        mModifiedFlags = Flag.getModifiedFlagsByOpcode(op.opcodeClass());
    }

    public Operand src() {
        return operand(0);
    }

    public Operand dest() {
        return operand(1);
    }
    
    public Set<Flag> modifiedFlags() {
        return mModifiedFlags;
    }
}
