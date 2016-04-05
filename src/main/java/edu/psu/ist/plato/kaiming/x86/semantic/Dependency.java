package edu.psu.ist.plato.kaiming.x86.semantic;

import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;

import edu.psu.ist.plato.kaiming.x86.*;

public class Dependency {
    /*
    public boolean conservative = false;

    public Dependency() {
        conservative = false;
    }
    
    public Dependency(boolean cons) {
        conservative = cons;
    }
    */
    
    public static final int NGenRegs = 8;
    public static final int NFlags = Flag.values().length;
    public static final int NBits = NGenRegs + NFlags + 2;
    
    public static final int MemIndex = NBits - 2;
    public static final int ImmIndex = NBits - 1;
    
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

    public static int getIndex(Register.Id reg) {
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

    public static int getIndex(Flag f) {
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

    public final void transferMultiplyInst(MultiplyInst inst, BitSet in) {
        boolean affected = false;
        for (Operand dest : inst.iterDest()) {
            affected = clearBitForOperand(dest, in) || affected;
        }
        if (affected) {
            Operand[] src = inst.iterSrc();
            for (int i = 0; i < src.length; ++i) {
                setBitForOperand(src[i], in);
            }
        }
    }
    
    public final void transferDivideInst(DivideInst inst, BitSet in) {
        boolean affected = false;
        for (Operand dest : inst.getDestIter()) {
            affected = clearBitForOperand(dest, in) || affected;
        }
        if (affected) {
            setBitForOperand(inst.getDivider(), in);
            for (Operand dividend : inst.getDividendIter())
                setBitForOperand(dividend, in);
        }
    }
    
    public final void transferBitTestInst(BitTestInst inst, BitSet in) {
        int idx = getIndex(Flag.CF);
        if (in.get(idx)) {
            in.clear(idx);
            Operand[] ops = inst.operands();
            for (int i = 0; i < ops.length; ++i) {
                setBitForOperand(ops[i], in);
            }
        }
    }
    
    public final void transferBinaryArithInst(BinaryArithInst inst, BitSet in) {
        boolean affected = clearBitForOperand(inst.dest(), in);
        Set<Flag> flags = inst.modifiedFlags();
        for (Flag f : flags) {
            int idx = getIndex(f);
            if (in.get(idx)) {
                in.clear(idx);
                affected = true;
            }
        }
        if (affected) {
            if (!inst.src().isImmeidate())
                setBitForOperand(inst.src(), in);
            setBitForOperand(inst.dest(), in);
        }
    }

    public final void transferCondSetInst(CondSetInst inst, BitSet in) {
        Operand op = inst.dest();
        if (!op.isRegister())
            return;
        Register reg = (Register)op;
        int idx = getIndex(reg.getContainingRegister().id);
        if (!in.get(idx))
            return;
        in.clear(idx);
        for (Flag f : inst.dependentFlags()) {
            in.set(getIndex(f));
        }
    }
    
    public final void transferJumpInst(JumpInst inst, BitSet in) {
        if (inst.isIndirect()) {
            // TODO: this should be fixed once we can analyze indirect jumps
            in.clear();
            return;
        }
        
        if (!inst.isConditional()) {
            return;
        }
        CondJumpInst cji = (CondJumpInst)inst;
        String rawop = cji.opcode().getRawOpcode();
        if (rawop.equals("jcxz") || rawop.equals("jecxz"))
            in.set(getIndex(Register.Id.ECX));
        for (Flag f : cji.getDependentFlags()) {
            in.set(getIndex(f));
        }
    }
    
    public final void transferCompareInst(CompareInst inst, BitSet in) {
        Set<Flag> modifiedFlags = inst.modifiedFlags();
        boolean affectted = false;
        for (Flag f : modifiedFlags) {
            int idx = getIndex(f);
            if (in.get(idx)) {
                affectted = true;
                in.clear(idx);
            }
        }
        if (affectted) {
            Operand[] ops = inst.operands();
            if (ops[0].isImmeidate() && ops[1].isImmeidate()) {
                // Could this ever happen?
                in.set(ImmIndex);
                return;
            }
            if (!ops[0].isImmeidate()) {
                setBitForOperand(ops[0], in);
            }
            if (!ops[1].isImmeidate()) {
                setBitForOperand(ops[1], in);
            }
        }
    }
    
    public final void transferExchangeInst(ExchangeInst inst, BitSet in) {
        Operand op1 = inst.operand(0), op2 = inst.operand(1);
        BitSet tmp = new BitSet();
        tmp.or(in);
        boolean affected1 = clearBitForOperand(op1, tmp);
        boolean affected2 = clearBitForOperand(op2, in);
        in.and(tmp);
        if (affected1)
            setBitForOperand(op2, in);
        if (affected2)
            setBitForOperand(op1, in);
    }
    
    public final void transferPopInst(PopInst inst, BitSet in) {
        Register dest = inst.popTarget().getContainingRegister();
        if (in.get(getIndex(dest.id))) {
            in.clear(getIndex(dest.id));
            in.set(getIndex(Register.Id.ESP));
            in.set(MemIndex);
        }
    }
    
    public final void transferMoveInst(MoveInst inst, BitSet in) {
        if (clearBitForOperand(inst.to(), in)) {
            setBitForOperand(inst.from(), in);
            if (inst.isConditional()) {
                CondMoveInst ci = (CondMoveInst)inst;
                for (Flag f : ci.getDependentFlags()) {
                    in.set(getIndex(f));
                }
            }
        }
    }
    
    public final void transferLeaInst(LeaInst inst, BitSet in) {
        Register result = inst.getResult().getContainingRegister();
        int index = getIndex(result.id);
        if (in.get(index)) {
            in.clear(index);
            Memory exp = inst.getExpression();
            if (exp.isConcrete())
                in.set(ImmIndex);
            setBitForOperand(exp.getBaseRegister(), in);
            setBitForOperand(exp.getOffsetRegister(), in);
        }
    }
    
    public static void setBitForOperand(Operand op, BitSet in) {
        if (op == null)
            return;
        
        switch (op.getType()) {
            case Register:{
                int idx = getIndex(op.asRegister().getContainingRegister().id);
                if (idx != -1)
                    in.set(idx);
                break;
            }
            case Memory: {
                in.set(MemIndex);
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
                if (mem.isConcrete()) {
                    in.set(ImmIndex);
                }
                break;
            }
            case Immediate:
                in.set(ImmIndex);
        }
    }
    
    public static boolean clearBitForOperand(Operand op, BitSet in) {
        boolean ret = false;
        switch (op.getType()) {
            case Register: {
                Register reg = op.asRegister().getContainingRegister();
                ret = in.get(getIndex(reg.id));
                in.clear(getIndex(reg.id));
                break;
            }
            case Memory:
                ret = in.get(MemIndex);
                break;
            case Immediate:
                break;
        }
        return ret;
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
