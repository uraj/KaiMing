package edu.psu.ist.plato.kaiming.arm64;

public abstract class CondInstruction extends Instruction {

    private Condition mCond;
    
    protected CondInstruction(Kind kind, long addr, Opcode op, Operand[] operands, Condition cond) {
        super(kind, addr, op, operands);
        mCond = cond;
    }

    public Condition condition() {
        return mCond;
    }
    
    public boolean always() {
        return mCond == Condition.AL;
    }
}
