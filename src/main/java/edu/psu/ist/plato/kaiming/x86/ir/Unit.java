package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.elf.Elf;

/**
 * A Unit is an object that holds the information of the disassembled
 * binary. Unit should be responsible for resolving the target of  
 * inter-procedural calls. 
 */
public class Unit {
    
    private Elf mElf;
    
    public Unit(Elf elf) {
        mElf = elf;
    }
    
    public Elf elf() {
        return mElf;
    }
}
