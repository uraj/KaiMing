package edu.psu.ist.plato.kaiming.x86;

public class Relocation extends Memory {

    private AsmLabel mLabel;
    public Relocation(Memory m, AsmLabel l) {
        super(m.getDisplacement(), m.getBaseRegister(),
                m.getOffsetRegister(), m.getMultiplier());
        mLabel = l;
    }

    public AsmLabel getLabel() { return mLabel; }
}
