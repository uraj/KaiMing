package edu.psu.ist.plato.kaiming;

import java.util.List;

import edu.psu.ist.plato.kaiming.arm64.ARM64Machine;
import edu.psu.ist.plato.kaiming.x86.X86Machine;

public abstract class Machine {
    public enum Arch {
        X86,
        ARM64,
    }
    
    public final Arch arch;
    
    protected Machine(Arch architecture) {
        arch = architecture;
    }
    
    public abstract List<MachRegister> registers();
    public abstract MachRegister returnRegister();
    public abstract int wordSizeInBits();
    public static final X86Machine x86 = X86Machine.instance;
    public static final ARM64Machine arm64 = ARM64Machine.instance;
}
