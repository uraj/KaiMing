package edu.psu.ist.plato.kaiming.x86;

public class JumpInst extends BranchInst {

    protected JumpInst(long addr, Opcode op, Memory target, boolean isIndirect) {
        super(addr, op, target, isIndirect);
    }
    
    protected JumpInst(long addr, Opcode op, Register target, boolean isIndirect) {
        super(addr, op, target, isIndirect);
    }

}
