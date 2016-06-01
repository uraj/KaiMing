package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.Label;

public abstract class BranchInst extends Instruction implements Entry.Terminator<Instruction> {

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
    
    @Override
    public final long targetIndex() {
    	if (!isIndirect() && isTargetConcrete())
    		return target().displacement();
    	else
    		throw new UnsupportedOperationException();
    }
    
    @Override
    public final boolean isIndirect() {
        return mIndirect;
    }
    
    @Override
    public final boolean isTargetConcrete() {
        if (mIndirect)
            return false;
        Memory target = target();
        return target.baseRegister() == null && target.offsetRegister() == null;
    }

    @Override
    public final void relocateTarget(BasicBlock<Instruction> target) {
        if (target == null || isIndirect() || !isTargetConcrete())
            throw new UnsupportedOperationException();
        setOperand(0, new Relocation(target(), target));
    }
    
    public final boolean isTargetRelocated() {
        return target().isRelocation();
    }

    public final Label targetLabel() {
        if (isTargetRelocated())
            return target().asRelocation().targetBlock().label();
        return null;
    }
}
