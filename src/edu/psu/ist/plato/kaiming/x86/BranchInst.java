package edu.psu.ist.plato.kaiming.x86;

public abstract class BranchInst extends Instruction {

    private final boolean mIndirect;
    
    protected BranchInst(long addr, Opcode op, Memory target, boolean isIndirect) {
        super(addr, op, new Operand[] { target });
        mIndirect = isIndirect;
    }
    
    protected BranchInst(long addr, Opcode op, Register target, boolean isIndirect) {
        super(addr, op, new Operand[] { new Memory(0, target, null, 0) });
        mIndirect = isIndirect;
    }
    
    public final Memory getTarget() {
        return (Memory)getOperand(0);
    }
    
    public final boolean isIndirect() {
        return mIndirect;
    }
    
    public final boolean isTargetConcrete() {
        return !mIndirect && getTarget().isImmeidate();
    }

    public final boolean relocateTarget(AsmLabel l) {
        if (l == null || isIndirect() || !isTargetConcrete())
            return false;
        setOperand(0, new Relocation(getTarget(), l));
        return true;
    }
}
