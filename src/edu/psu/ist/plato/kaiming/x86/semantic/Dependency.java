package edu.psu.ist.plato.kaiming.x86.semantic;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import edu.psu.ist.plato.kaiming.x86.BinaryArithInst;
import edu.psu.ist.plato.kaiming.x86.BitTestInst;
import edu.psu.ist.plato.kaiming.x86.CompareInst;
import edu.psu.ist.plato.kaiming.x86.CondJumpInst;
import edu.psu.ist.plato.kaiming.x86.CondMoveInst;
import edu.psu.ist.plato.kaiming.x86.CondSetInst;
import edu.psu.ist.plato.kaiming.x86.DivideInst;
import edu.psu.ist.plato.kaiming.x86.ExchangeInst;
import edu.psu.ist.plato.kaiming.x86.Flag;
import edu.psu.ist.plato.kaiming.x86.JumpInst;
import edu.psu.ist.plato.kaiming.x86.LeaInst;
import edu.psu.ist.plato.kaiming.x86.Memory;
import edu.psu.ist.plato.kaiming.x86.MoveInst;
import edu.psu.ist.plato.kaiming.x86.MultiplyInst;
import edu.psu.ist.plato.kaiming.x86.Operand;
import edu.psu.ist.plato.kaiming.x86.PopInst;
import edu.psu.ist.plato.kaiming.x86.Register;

public class Dependency {
    public static final int NGenRegs = 8;
    public static final int sNFlags = Flag.values().length;
    public static final int sNBits = NGenRegs + sNFlags;
    
    private static final Set<Register.Id> sGeneralRegs;
    private static final Set<Flag> sFlags;
    
    static {
        sGeneralRegs = new HashSet<Register.Id>();
        sGeneralRegs.add(Register.Id.EAX);
        sGeneralRegs.add(Register.Id.EBX);
        sGeneralRegs.add(Register.Id.ECX);
        sGeneralRegs.add(Register.Id.EDX);
        sGeneralRegs.add(Register.Id.ESI);
        sGeneralRegs.add(Register.Id.EDI);
        sGeneralRegs.add(Register.Id.ESP);
        sGeneralRegs.add(Register.Id.EBP);
        
        sFlags = new HashSet<Flag>();
        sFlags.add(Flag.AF);
        sFlags.add(Flag.CF);
        sFlags.add(Flag.OF);
        sFlags.add(Flag.PF);
        sFlags.add(Flag.ZF);
        sFlags.add(Flag.SF);
        sFlags.add(Flag.TF);
        sFlags.add(Flag.DF);
        sFlags.add(Flag.IF);
    }

    private static int getIndex(Register.Id reg) {
        if (reg == null)
            return -1;
        int ret;
        switch (reg) {
            case EAX: ret = 0; break;
            case EBX: ret = 1; break;
            case ECX: ret = 2; break;
            case EDX: ret = 3; break;
            case ESI: ret = 4; break;
            case EDI: ret = 5; break;
            case ESP: ret = 6; break;
            case EBP: ret = 7; break;
            default:
                ret = -1;
        }
        return ret;
    }

    private static int getIndex(Flag f) {
        if (f == null)
            return -1;
        int ret;
        switch (f) {
            case CF: ret = 0; break;
            case PF: ret = 1; break;
            case AF: ret = 2; break;
            case ZF: ret = 3; break;
            case SF: ret = 4; break;
            case TF: ret = 5; break;
            case IF: ret = 6; break;
            case DF: ret = 7; break;
            case OF: ret = 8; break;
            default:
                ret = -NGenRegs - 1;
        }
        return ret + NGenRegs;
    }

    public static void transferMultiplyInst(MultiplyInst inst, BitSet in) {
        boolean affected = false;
        Operand[] dest = inst.getDest();
        for (int i = 0; i < dest.length; ++i) {
            if (dest[i].isRegister()) {
                affected = true;
                Register reg = (Register)dest[i];
                in.clear(getIndex(reg.getContainingRegister().id));
            }
        }
        if (affected) {
            Operand[] src = inst.getSrc();
            for (int i = 0; i < src.length; ++i) {
                setBitForOperand(src[i], in);
            }
        }
    }
    
    public static void transferDivideInst(DivideInst inst, BitSet in) {
        boolean affected = false;
        Operand[] dest = inst.getDest();
        for (int i = 0; i < dest.length; ++i) {
            if (dest[i].isRegister()) {
                affected = true;
                Register reg = (Register)dest[i];
                in.clear(getIndex(reg.getContainingRegister().id));
            }
        }
        if (affected) {
            setBitForOperand(inst.getDivider(), in);
            Operand[] dividends = inst.getDividend();
            for (int i = 0; i < dividends.length; ++i)
                setBitForOperand(dividends[i], in);
        }
    }
    
    public static void transferBitTestInst(BitTestInst inst, BitSet in) {
        int idx = getIndex(Flag.CF);
        if (in.get(idx)) {
            in.clear(idx);
            Operand[] ops = inst.getOperands();
            for (int i = 0; i < ops.length; ++i) {
                setBitForOperand(ops[i], in);
            }
        }
    }
    
    public static void transferBinaryArithInst(BinaryArithInst inst, BitSet in) {
        Operand dest = inst.getDest();
        boolean affected = false;
        int idx;
        if (dest.isRegister()) {
            idx = getIndex(dest.asRegister().getContainingRegister().id);
            if (in.get(idx)) {
                in.clear(idx);
                affected = true;
            }
        }
        Set<Flag> flags = inst.getModifiedFlags();
        for (Flag f : flags) {
            idx = getIndex(f);
            if (in.get(idx)) {
                in.clear(idx);
                affected = true;
            }
        }
        if (affected) {
            setBitForOperand(inst.getSrc(), in);
            setBitForOperand(inst.getDest(), in);
        }
    }

    public static void transferCondSetInst(CondSetInst inst, BitSet in) {
        Operand op = inst.getDest();
        if (!op.isRegister())
            return;
        Register reg = (Register)op;
        int idx = getIndex(reg.getContainingRegister().id);
        if (!in.get(idx))
            return;
        in.clear(idx);
        for (Flag f : inst.getDependentFlags()) {
            in.set(getIndex(f));
        }
    }
    
    public static void transferJumpInst(JumpInst inst, BitSet in) {
        if (!inst.isConditional())
            return;
        CondJumpInst cji = (CondJumpInst)inst;
        String rawop = cji.getOpcode().getRawOpcode();
        if (rawop.equals("jcxz") || rawop.equals("jecxz"))
            in.set(getIndex(Register.Id.ECX));
        for (Flag f : cji.getDependentFlags()) {
            in.set(getIndex(f));
        }
    }
    
    public static void transferCompareInst(CompareInst inst, BitSet in) {
        Set<Flag> modifiedFlags = inst.getModifiedFlags();
        boolean affectted = false;
        for (Flag f : modifiedFlags) {
            int idx = getIndex(f);
            if (in.get(idx)) {
                affectted = true;
                in.clear(idx);
            }
        }
        if (affectted) {
            Operand[] ops = inst.getOperands();
            if (ops[0].isImmeidate() && ops[1].isImmeidate()) {
                return;
            }
            for (int i = 0; i < ops.length; ++i) {
                setBitForOperand(ops[i], in);
            }
        }
    }
    
    public static void transferExchangeInst(ExchangeInst inst, BitSet in) {
        boolean affected1 = false, affected2 = false;
        Operand op1 = inst.getOperand(0), op2 = inst.getOperand(1), op;
        int idx1  = -1, idx2 = -1;
        if (op1.isRegister()) {
            int tmp = getIndex(((Register)op1).getContainingRegister().id);
            if (in.get(tmp)) {
                affected1 = true;
                idx1 = tmp;
            }
        }
        if (op2.isRegister()) {
            int tmp = getIndex(((Register)op2).getContainingRegister().id);
            if (in.get(tmp)) {
                affected2 = true;
                idx2 = tmp;
            }
        }
        if (!(affected1 ^ affected2))
            return;

        in.clear(affected1 ? idx1 : idx2);
        
        op = affected1 ? op2 : op1;
        setBitForOperand(op, in);
    }
    
    public static void transferPopInst(PopInst inst, BitSet in) {
        Register dest = inst.getTarget();
        in.clear(getIndex(dest.getContainingRegister().id));
        in.set(getIndex(Register.Id.ESP));
    }
    
    public static void transferMoveInst(MoveInst inst, BitSet in) {
        Operand dest = inst.getTo();
        int index;
        if (dest.isRegister()) {
            index = getIndex(dest.asRegister().getContainingRegister().id);
            if (in.get(index)) {
                in.clear(index);
                Operand src = inst.getFrom();
                setBitForOperand(src, in);
                if (inst.isConditional()) {
                    CondMoveInst ci = (CondMoveInst)inst;
                    for (Flag f : ci.getDependentFlags()) {
                        in.set(getIndex(f));
                    }
                }
            }
        }
    }
    
    public static void transferLeaInst(LeaInst inst, BitSet in) {
        Register result = inst.getResult().asRegister().getContainingRegister();
        int index = getIndex(result.id);
        if (in.get(index)) {
            in.clear(index);
            Memory exp = inst.getExpression();
            setBitForOperand(exp, in);
        }
    }
    
    public static void setBitForOperand(Operand op, BitSet in) {
        if(op.isRegister()) {
            int idx = getIndex(op.asRegister().getContainingRegister().id);
            if (idx != -1)
                in.set(idx);
        } else if (op.isMemory()) {
            Memory mem = (Memory)op;
            Register reg;
            reg = mem.getBaseRegister();
            if (reg != null) {
                int idx = getIndex(reg.getContainingRegister().id);
                if (idx != -1)
                    in.set(idx);
            }
            reg = mem.getOffsetRegister();
            if (reg != null) {
                int idx = getIndex(reg.getContainingRegister().id);
                if (idx != -1)
                    in.set(getIndex(reg.getContainingRegister().id));
            }
        }
    }
    
    public static Set<Register.Id> bitSetToRegisterSet(BitSet in) {
        Set<Register.Id> ret = new HashSet<Register.Id>();
        for (Register.Id id : sGeneralRegs) {
            if (in.get(getIndex(id)))
                ret.add(id);
        }
        return ret;
    }
    
    public static Set<Flag> bitSetToFlagSet(BitSet in) {
        Set<Flag> ret = new HashSet<Flag>();
        for (Flag f : sFlags) {
            if (in.get(getIndex(f)))
                ret.add(f);
        }
        return ret;
    }
}
