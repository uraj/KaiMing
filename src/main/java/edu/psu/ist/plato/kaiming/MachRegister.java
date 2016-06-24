package edu.psu.ist.plato.kaiming;

import java.util.Set;

import edu.psu.ist.plato.kaiming.Machine.Arch;

public abstract class MachRegister {
    abstract public String name();
    abstract public Arch arch();
    abstract public int sizeInBits();
    abstract public MachRegister containingRegister();
    abstract public Set<MachRegister> subsumedRegisters();
    
    @Override
    public abstract boolean equals(Object that); 
}
