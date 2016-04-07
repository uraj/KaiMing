package edu.psu.ist.plato.kaiming.x86;

import java.util.Collections;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Map;
import java.util.SortedSet;

public enum Flag {
    CF(0), PF(2), AF(4), ZF(6), SF(7), TF(8), IF(9), DF(10), OF(11);

    private final int mPos;
    private static final Flag[] sFlags;
    static {
        sFlags = new Flag[32];
        for (Flag f : Flag.values()) {
            sFlags[f.bitPosition()] = f;
        }
    }

    private Flag(int pos) {
        mPos = pos;
    }

    public int bitPosition() {
        return mPos;
    }

    public static Flag getFlag(int pos) {
        if (pos < 0 || pos >= sFlags.length)
            return null;
        return sFlags[pos];
    }
    
    static private final Map<String, SortedSet<Flag>> sCondLookup;
    static private final Map<Opcode.Class, SortedSet<Flag>> sOpcodeLookup;
    
    static {
        sCondLookup = new HashMap<String, SortedSet<Flag>>();
        SortedSet<Flag> v, imv;
        
        v = new TreeSet<Flag>();
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("", imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.OF);
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("o", imv);
        sCondLookup.put("no", imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.SF);
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("s", imv);
        sCondLookup.put("ns", imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.ZF);
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("e", imv);
        sCondLookup.put("z", imv);
        sCondLookup.put("ne", imv);
        sCondLookup.put("nz", imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.CF);
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("b", imv);
        sCondLookup.put("nae", imv);
        sCondLookup.put("c", imv);
        sCondLookup.put("nb", imv);
        sCondLookup.put("ae", imv);
        sCondLookup.put("nc", imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.CF);
        v.add(Flag.ZF);
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("be", imv);
        sCondLookup.put("na", imv);
        sCondLookup.put("nbe", imv);
        sCondLookup.put("a", imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.OF);
        v.add(Flag.SF);
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("l", imv);
        sCondLookup.put("nge", imv);
        sCondLookup.put("nl", imv);
        sCondLookup.put("ge", imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.ZF);
        v.add(Flag.SF);
        v.add(Flag.OF);
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("le", imv);
        sCondLookup.put("ng", imv);
        sCondLookup.put("nle", imv);
        sCondLookup.put("g", imv);

        v = new TreeSet<Flag>();
        v.add(Flag.PF);
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("p", imv);
        sCondLookup.put("pe", imv);
        sCondLookup.put("np", imv);
        sCondLookup.put("po", imv);
        
        v = new TreeSet<Flag>();
        imv = Collections.unmodifiableSortedSet(v);
        sCondLookup.put("cxz", imv);
        sCondLookup.put("ecxz", imv);
    }
    
    static {
        sOpcodeLookup = new HashMap<Opcode.Class, SortedSet<Flag>>();
        SortedSet<Flag> v, imv;
        
        v = new TreeSet<Flag>();
        v.add(Flag.OF);
        v.add(Flag.CF);
        v.add(Flag.SF);
        v.add(Flag.ZF);
        v.add(Flag.PF);
        v.add(Flag.AF);
        imv = Collections.unmodifiableSortedSet(v);
        sOpcodeLookup.put(Opcode.Class.ADD, imv);
        sOpcodeLookup.put(Opcode.Class.ADC, imv);
        sOpcodeLookup.put(Opcode.Class.SUB, imv);
        sOpcodeLookup.put(Opcode.Class.SBB, imv);
        sOpcodeLookup.put(Opcode.Class.NEG, imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.OF);
        v.add(Flag.CF);
        imv = Collections.unmodifiableSortedSet(v);
        sOpcodeLookup.put(Opcode.Class.MUL, imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.OF);
        v.add(Flag.SF);
        v.add(Flag.ZF);
        v.add(Flag.PF);
        v.add(Flag.AF);
        imv = Collections.unmodifiableSortedSet(v);
        sOpcodeLookup.put(Opcode.Class.INC, imv);
        sOpcodeLookup.put(Opcode.Class.DEC, imv);
        
        v = new TreeSet<Flag>();
        v.add(Flag.OF);
        v.add(Flag.CF);
        v.add(Flag.SF);
        v.add(Flag.ZF);
        v.add(Flag.PF);
        imv = Collections.unmodifiableSortedSet(v);
        sOpcodeLookup.put(Opcode.Class.AND, imv);
        sOpcodeLookup.put(Opcode.Class.OR, imv);
        sOpcodeLookup.put(Opcode.Class.XOR, imv);
        sOpcodeLookup.put(Opcode.Class.SAR, imv);
        sOpcodeLookup.put(Opcode.Class.SHL, imv);
        sOpcodeLookup.put(Opcode.Class.SHR, imv);
        
        v = new TreeSet<Flag>();
        imv = Collections.unmodifiableSortedSet(v);
        sOpcodeLookup.put(Opcode.Class.DIV, imv);
        sOpcodeLookup.put(Opcode.Class.LEA, imv);
        sOpcodeLookup.put(Opcode.Class.NOT, imv);
        sOpcodeLookup.put(Opcode.Class.SETCC, imv);
        sOpcodeLookup.put(Opcode.Class.BSWAP, imv);
    }
    
    public static SortedSet<Flag> getDependentFlagsByCondition(String condition) {
        return sCondLookup.get(condition);
    }
    
    public static SortedSet<Flag> getModifiedFlagsByOpcode(Opcode.Class op) {
        return sOpcodeLookup.get(op);
    }

}
