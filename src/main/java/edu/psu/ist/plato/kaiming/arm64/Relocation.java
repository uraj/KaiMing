package edu.psu.ist.plato.kaiming.arm64;

import edu.psu.ist.plato.kaiming.BasicBlock;

public class Relocation extends Memory {

    private BasicBlock<Instruction> mTargetBB;
    public Relocation(BasicBlock<Instruction> bb) {
        super(null, bb.label().addr());
        mTargetBB = bb;
    }
    
    public BasicBlock<Instruction> targetBlock() {
        return mTargetBB;
    }

}
