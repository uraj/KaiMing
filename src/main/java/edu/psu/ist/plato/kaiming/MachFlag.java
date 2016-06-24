package edu.psu.ist.plato.kaiming;

import edu.psu.ist.plato.kaiming.Machine.Arch;

public interface MachFlag {
    public String name();
    public Arch arch();
    public int index();
    
    @Override
    public abstract boolean equals(Object that);
}

