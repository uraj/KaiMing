package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Register;

public class Reg extends Lval {
    private Register mReg;

    private Reg(Register reg) {
        mReg = reg;
    }
    
    public Register x86Register() {
        return mReg;
    }
    
    public static Reg esp = new Reg(Register.getRegister(Register.Id.ESP));
    public static Reg ebp = new Reg(Register.getRegister(Register.Id.EBP));
    public static Reg eax = new Reg(Register.getRegister(Register.Id.EAX));
    public static Reg ebx = new Reg(Register.getRegister(Register.Id.EBX));
    public static Reg ecx = new Reg(Register.getRegister(Register.Id.ECX));
    public static Reg edx = new Reg(Register.getRegister(Register.Id.EDX));
    public static Reg edi = new Reg(Register.getRegister(Register.Id.EDI));
    public static Reg esi = new Reg(Register.getRegister(Register.Id.ESI));
    
    public static Reg getReg(Register register) {
        switch (register.id) {
            case EAX: return eax;
            case EBX: return ebx;
            case ECX: return ecx;
            case EDX: return edx;
            case ESP: return esp;
            case EBP: return ebp;
            case EDI: return edi;
            case ESI: return esi;
            default: return new Reg(register);
        }
    }

    
    public boolean equalsTo(Reg reg) {
        return mReg.id == reg.mReg.id;
    }
    
    @Override
    public boolean equals(Object lv) {
        if (lv instanceof Reg) {
            return equalsTo((Reg)lv);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mReg.id.hashCode();
    }

    @Override
    public int sizeInBits() {
        return mReg.sizeInBits();
    }
}
