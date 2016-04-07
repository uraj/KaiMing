package edu.psu.ist.plato.kaiming.x86;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class CompareInst extends Instruction {
    
    private static final Set<Flag> sCompareFlagSet;
    
    static {
        Set<Flag> cmpSet = new HashSet<Flag>();
        cmpSet.add(Flag.SF);
        cmpSet.add(Flag.ZF);
        cmpSet.add(Flag.PF);
        cmpSet.add(Flag.CF);
        cmpSet.add(Flag.OF);
        cmpSet.add(Flag.AF);
        sCompareFlagSet = Collections.unmodifiableSet(cmpSet);
    }

    protected CompareInst(long addr, Opcode op, Operand cmp1, Operand cmp2) {
        super(Kind.COMPARE, addr, op, new Operand[] {cmp1, cmp2});
    }
    
    public boolean isTest() {
        return opcode().opcodeClass() == Opcode.Class.TEST;
    }
    
    public boolean isCompare() {
        return opcode().opcodeClass() == Opcode.Class.CMP;
    }
    
    @Override
    public Set<Flag> modifiedFlags() {
        return sCompareFlagSet;
    }
}
