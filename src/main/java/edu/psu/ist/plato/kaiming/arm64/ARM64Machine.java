package edu.psu.ist.plato.kaiming.arm64;

import java.util.ArrayList;
import java.util.List;

import edu.psu.ist.plato.kaiming.Machine;

public class ARM64Machine extends Machine {

    public static final ARM64Machine instance = new ARM64Machine();
    
    protected ARM64Machine() {
        super(Arch.ARM64);
    }

    @Override
    public List<MachRegister> registers() {
        Register.Id[] allRegs = Register.Id.values();
        List<MachRegister> ret = new ArrayList<MachRegister>();
        for (Register.Id id : allRegs) {
            ret.add(Register.getRegister(id));
        }
        return null;
    }

    @Override
    public MachRegister returnRegister() {
        return Register.getRegister("R0");
    }

}
