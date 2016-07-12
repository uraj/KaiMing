package edu.psu.ist.plato.kaiming.arm64;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.util.Tuple;

public class Opcode {
    public enum Mnemonic {
        ADD, SUB, MUL, DIV,
        ADR,
        ASR, LSL, LSR,
        ORR, ORN, NEG, AND,
        LDR, LDP, STR, STP,
        CMP, CMN,
        CSEL, CSINC,
        CINC, CSET, // aliases to CSINC
        MOV, MOVK,
        B, BL,
        NOP,
        EXT,
        BFM,
        TST,
    }

    private static Map<String, Mnemonic> sMap = new HashMap<String, Mnemonic>();
    
    static {
        List<Tuple<Mnemonic, String[]>> initList = 
                new ArrayList<Tuple<Mnemonic, String[]>>();
        initList.add(new Tuple<Mnemonic, String[]>(
            Mnemonic.LDR,
            new String[]{ "LDR", "LDUR", "LDRSW", "LDURB", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.LDP,
                new String[]{ "LDP", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.STP,
                new String[]{ "STP", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.STR,
                new String[] { "STR", "STUR", "SXTW", "STURB", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.ADD,
                new String[] { "ADD", "ADDS", "ADC", "ADCS", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.SUB,
                new String[] { "SUB", "SUBS", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.ADR,
                new String[] { "ADR", "ADRP", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.AND,
                new String[] { "AND", "ANDS", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.TST,
                new String[] { "TST", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.ASR,
                new String[] { "ASR", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.B,
                new String[] { "B", "BR", "RET", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.BL,
                new String[] { "BL", "BLR", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.CMP,
                new String[] { "CMP", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.CMN,
                new String[] { "CMN", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.CSEL,
                new String[] { "CSEL", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.CSINC,
                new String[] { "CSINC", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.CINC,
                new String[] { "CINC", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.CSET,
                new String[] { "CSET", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.LSL,
                new String[] { "LSL", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.LSR,
                new String[] { "LSR", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.MOV,
                new String[] { "MOV", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.MOVK,
                new String[] { "MOVK", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.EXT,
                new String[] { "SXTW", "SXTH", "SXTB", "UXTW", "UXTH", "UXTB", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.BFM,
                new String[] { "SBFX", "SBFM", "UBFX", "UBFM", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.MUL,
                new String[] { "MUL", "UMUL", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.DIV,
                new String[] { "SDIV", "UDIV", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.NEG,
                new String[] { "NEG",  }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.ORR,
                new String[] { "ORR", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.ORN,
                new String[] { "ORN", }));
        initList.add(new Tuple<Mnemonic, String[]>(
                Mnemonic.NOP,
                new String[] { "NOP", }));
        for (Tuple<Mnemonic, String[]> initItem : initList) {
            for (String str : initItem.second) {
                sMap.put(str, initItem.first);
            }
        }
    }
    
    private String mCode;
    private Mnemonic mClass;
    
    public Opcode(String rawcode) {
        mCode = rawcode.toUpperCase();
        mClass = sMap.get(mCode.split("\\.")[0]);
        Assert.verify(mClass != null, "Unknown opcode: " + rawcode);
    }
    
    public String rawOpcode() {
        return mCode;
    }
    
    public Condition getCondition() {
        String[] parts = mCode.split("\\.");
        Assert.verify(parts.length <= 2);
        Condition ret = Condition.AL;
        if (parts.length == 2) {
            Assert.verify(parts[0].equals("B"));
            ret = Condition.valueOf(parts[1].toUpperCase());
        }
        return ret;
    }
    
    public Mnemonic mnemonic() {
        return mClass;
    }
}
