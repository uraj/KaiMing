package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.Label;

public abstract class BranchInst extends Instruction {

    private final boolean mIndirect;
    
    protected BranchInst(Kind kind, long addr, Opcode op, Memory target, boolean isIndirect) {
        super(kind, addr, op, new Operand[] { target });
        mIndirect = isIndirect;
    }
    
    protected BranchInst(Kind kind, long addr, Opcode op, Register target, boolean isIndirect) {
        super(kind, addr, op, new Operand[] { new Memory(0, target, null, 0) });
        mIndirect = isIndirect;
    }
    
    public final Memory target() {
        return operand(0).asMemory();
    }
    
    public final boolean isIndirect() {
        return mIndirect;
    }
    
    public final boolean isTargetConcrete() {
        if (mIndirect)
            return false;
        Memory target = target();
        return target.baseRegister() == null && target.offsetRegister() == null;
    }

    public final boolean relocateTarget(Label l) {
        if (l == null || isIndirect() || !isTargetConcrete())
            return false;
        setOperand(0, new Relocation(target(), l));
        return true;
    }
}
