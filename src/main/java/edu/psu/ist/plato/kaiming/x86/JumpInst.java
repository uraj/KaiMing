package edu.psu.ist.plato.kaiming.x86;

import java.util.List;

import edu.psu.ist.plato.kaiming.x86.ir.Stmt;

public class JumpInst extends BranchInst {

    protected JumpInst(long addr, Opcode op, Memory target, boolean isIndirect) {
        super(addr, op, target, isIndirect);
    }
    
    protected JumpInst(long addr, Opcode op, Register target, boolean isIndirect) {
        super(addr, op, target, isIndirect);
    }

    @Override
    List<Stmt> toIR() {
        // TODO Auto-generated method stub
        return null;
    }

}
