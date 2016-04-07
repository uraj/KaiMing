package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.BasicBlock;

public class Relocation extends Memory {

    private BasicBlock<Instruction> mTB;
    public Relocation(Memory m, BasicBlock<Instruction> target) {
        super(m.displacement(), m.baseRegister(),
                m.offsetRegister(), m.multiplier());
        mTB = target;
    }

    public BasicBlock<Instruction> targetBlock() {
        return mTB;
    }
    
    @Override
    public boolean isRelocation() {
        return true;
    }
}
