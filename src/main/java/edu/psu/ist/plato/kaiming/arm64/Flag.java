package edu.psu.ist.plato.kaiming.arm64;

import edu.psu.ist.plato.kaiming.Machine.Arch;
import edu.psu.ist.plato.kaiming.MachFlag;

public enum Flag implements MachFlag {
    
    N(0),
    Z(1),
    C(2),
    V(3);

    public final int index;
    
    private Flag(int idx) {
        index = idx;
    }

    @Override
    public Arch arch() {
        return Arch.ARM64;
    }

    @Override
    public int index() {
        return index;
    }
    
}
