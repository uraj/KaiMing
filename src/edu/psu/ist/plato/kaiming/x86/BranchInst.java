package edu.psu.ist.plato.kaiming.x86;

public abstract class BranchInst extends Instruction {

    private final boolean mIndirect;
    
    protected BranchInst(long addr, Opcode op, Operand target, boolean isIndirect) {
        super(addr, op, new Operand[] { target });
        mIndirect = isIndirect;
    }
    
    public final Memory getTarget() {
        return (Memory)getOperand(0);
    }
    
    public final boolean isIndirect() {
        return mIndirect;
    }
    
    public final boolean isTargetConcrete() {
        return !mIndirect && getTarget().isConcrete();
    }

    public final boolean relocateTarget(AsmLabel l) {
        if (l == null || isIndirect() || !isTargetConcrete())
            return false;
        setOperand(0, new Relocation(getTarget(), l));
        return true;
    }
}
