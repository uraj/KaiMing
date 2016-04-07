package edu.psu.ist.plato.kaiming.x86;

import edu.psu.ist.plato.kaiming.Label;

public class Relocation extends Memory {

    private Label mLabel;
    public Relocation(Memory m, Label l) {
        super(m.displacement(), m.baseRegister(),
                m.offsetRegister(), m.multiplier());
        mLabel = l;
    }

    public Label label() { return mLabel; }
    
    @Override
    public boolean isRelocation() {
        return true;
    }
}
