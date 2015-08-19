package edu.psu.ist.plato.kaiming.x86;

public class JumpInst extends BranchInst {

    protected JumpInst(long addr, Opcode op, Operand target, boolean isIndirect) {
        super(addr, op, target, isIndirect);
    }

}
