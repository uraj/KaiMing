package edu.psu.ist.plato.kaiming.arm64;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.psu.ist.plato.kaiming.Machine;
import edu.psu.ist.plato.kaiming.ir.Context;
import edu.psu.ist.plato.kaiming.ir.Stmt;

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

    @Override
    public int wordSizeInBits() {
        return 64;
    }
    
    public List<Stmt> toIRStatements(Context ctx, Instruction inst) {
        LinkedList<Stmt> ret = new LinkedList<Stmt>();
        switch (inst.kind()) {
            case BIN_ARITHN:
                break;
            case BITFIELD_MOVE:
                break;
            case BRANCH:
                break;
            case COMPARE:
                break;
            case LOAD:
                break;
            case LOAD_PAIR:
                break;
            case MOVE:
                break;
            case NOP:
                break;
            case POP:
                break;
            case PUSH:
                break;
            case SELECT:
                break;
            case STORE:
                break;
            case STORE_PAIR:
                break;
            case UN_ARITH:
                break;
            default:
                break;
        }
        return ret;
    }

}
