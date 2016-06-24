package edu.psu.ist.plato.kaiming;

import java.util.Set;

import edu.psu.ist.plato.kaiming.Machine.Arch;

public abstract class MachRegister {
    abstract public String name();
    abstract public Arch arch();
    abstract public int sizeInBits();
    // TODO: Return every containing register rather than the first one.
    // This is critical for the x86 architecture
    abstract public MachRegister containingRegister();
    abstract public Set<MachRegister> subsumedRegisters();
    
    @Override
    public abstract boolean equals(Object that);
    
    @Override
    public abstract int hashCode();
}
