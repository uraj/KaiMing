package edu.psu.ist.plato.kaiming.x86;

import java.util.HashMap;
import java.util.Map;

public class Opcode {
    public enum Class {
        // binary arithmetic
        ADD, ADC, SUB, SBB, MUL, DIV, AND, XOR, OR, SAR, SHL, SHR, LEA,
        // test
        TEST, 
        // compare
        CMP,
        // bit test
        BT,
        // exchange
        XCHG,
        // unary arithmetic
        INC, DEC, NEG, NOT, BSWAP, SETCC,
        // move
        MOV,
        // conditional move
        CMOV,
        // 
        PUSH, POP,
        // invocation
        CALL,
        // jump
        JMP,
        // conditional jump
        JCC,
        // return
        RET,
        // nop
        NOP,
        // rep
        REP,
        // others we don't really care right now
        LEAVE, EXT, CPUID, INT, HALT, UD,
    };

    private static Map<String, Class> sMap = new HashMap<String, Class>();

    static {
        // Binary Arithmetics
        sMap.put("add", Class.ADD);
        sMap.put("addl", Class.ADD);
        sMap.put("adc", Class.ADC);
        sMap.put("sub", Class.SUB);
        sMap.put("subl", Class.SUB);
        sMap.put("sbb", Class.SBB);
        sMap.put("mul", Class.MUL);
        sMap.put("imul", Class.MUL);
        sMap.put("div", Class.DIV);
        sMap.put("divl", Class.DIV);
        sMap.put("idiv", Class.DIV);
        sMap.put("and", Class.AND);
        sMap.put("andl", Class.AND);
        sMap.put("andb", Class.AND);
        sMap.put("or", Class.OR);
        sMap.put("orl", Class.OR);
        sMap.put("xor", Class.XOR);
        
        sMap.put("bt", Class.BT);
        
        sMap.put("cmp", Class.CMP);
        sMap.put("cmps", Class.CMP);
        sMap.put("scas", Class.CMP);
        sMap.put("cmpl", Class.CMP);
        sMap.put("cmpb", Class.CMP);
        sMap.put("cmpw", Class.CMP);
        
        sMap.put("test", Class.TEST);
        sMap.put("testb", Class.TEST);
        
        // Exchange
        sMap.put("xchg", Class.XCHG);
        sMap.put("xadd", Class.XCHG);

        sMap.put("lea", Class.LEA);
        
        // Unary arithmetic
        sMap.put("neg", Class.NEG);
        sMap.put("not", Class.NOT);
        sMap.put("seta", Class.SETCC);
        sMap.put("setb", Class.SETCC);
        sMap.put("setbe", Class.SETCC);
        sMap.put("sete", Class.SETCC);
        sMap.put("setg", Class.SETCC);
        sMap.put("setl", Class.SETCC);
        sMap.put("setne", Class.SETCC);
        sMap.put("sar", Class.SAR);
        sMap.put("shl", Class.SHL);
        sMap.put("shr", Class.SHR);
        sMap.put("dec", Class.DEC);
        sMap.put("decb", Class.DEC);
        sMap.put("decl", Class.DEC);
        sMap.put("decw", Class.DEC);
        sMap.put("inc", Class.INC);
        sMap.put("incb", Class.INC);
        sMap.put("incl", Class.INC);
        sMap.put("incw", Class.INC);
        sMap.put("bswap", Class.BSWAP);
        
        // Move
        sMap.put("mov", Class.MOV);
        sMap.put("movl", Class.MOV);
        sMap.put("movw", Class.MOV);
        sMap.put("movb", Class.MOV);
        sMap.put("movz", Class.MOV);
        sMap.put("movzbl", Class.MOV);
        sMap.put("movzwl", Class.MOV);
        sMap.put("movzbw", Class.MOV);
        sMap.put("movs", Class.MOV);
        sMap.put("movsbl", Class.MOV);
        sMap.put("movswl", Class.MOV);
        sMap.put("movsbw", Class.MOV);
        sMap.put("stos", Class.MOV);
        sMap.put("lods", Class.MOV);

        // Conditional move
        sMap.put("cmova", Class.CMOV);
        sMap.put("cmovae", Class.CMOV);
        sMap.put("cmovbe", Class.CMOV);
        sMap.put("cmove", Class.CMOV);
        sMap.put("cmovg", Class.CMOV);
        sMap.put("cmovl", Class.CMOV);
        sMap.put("cmovle", Class.CMOV);
        sMap.put("cmovne", Class.CMOV);
        sMap.put("cmovns", Class.CMOV);
        sMap.put("cmovs", Class.CMOV);

        // Jump
        sMap.put("jmp", Class.JMP);

        // Conditional jump
        sMap.put("jz", Class.JCC);
        sMap.put("ja", Class.JCC);
        sMap.put("jae", Class.JCC);
        sMap.put("jb", Class.JCC);
        sMap.put("jbe", Class.JCC);
        sMap.put("jc", Class.JCC);
        sMap.put("je", Class.JCC);
        sMap.put("jg", Class.JCC);
        sMap.put("jge", Class.JCC);
        sMap.put("jl", Class.JCC);
        sMap.put("jle", Class.JCC);
        sMap.put("jna", Class.JCC);
        sMap.put("jnae", Class.JCC);
        sMap.put("jnb", Class.JCC);
        sMap.put("jnbe", Class.JCC);
        sMap.put("jnc", Class.JCC);
        sMap.put("jne", Class.JCC);
        sMap.put("jng", Class.JCC);
        sMap.put("jnge", Class.JCC);
        sMap.put("jnl", Class.JCC);
        sMap.put("jnle", Class.JCC);
        sMap.put("jno", Class.JCC);
        sMap.put("jnp", Class.JCC);
        sMap.put("jns", Class.JCC);
        sMap.put("jnz", Class.JCC);
        sMap.put("jo", Class.JCC);
        sMap.put("jp", Class.JCC);
        sMap.put("jpe", Class.JCC);
        sMap.put("jpo", Class.JCC);
        sMap.put("js", Class.JCC);
        
        sMap.put("rep movs", Class.REP);
        sMap.put("rep movsl", Class.REP);
        sMap.put("rep lods", Class.REP);
        sMap.put("rep stos", Class.REP);
        sMap.put("rep cmps", Class.REP);
        sMap.put("rep scas", Class.REP);
        sMap.put("repz cmps", Class.REP);
        sMap.put("repz scas", Class.REP);
        sMap.put("repe cmps", Class.REP);
        sMap.put("repe scas", Class.REP);
        sMap.put("repnz cmps", Class.REP);
        sMap.put("repnz scas", Class.REP);
        sMap.put("repne cmps", Class.REP);
        sMap.put("repne scas", Class.REP);

        // Call
        sMap.put("call", Class.CALL);

        // Push
        sMap.put("push", Class.PUSH);
        sMap.put("pushl", Class.PUSH);
        
        // Pop
        sMap.put("pop", Class.POP);
        sMap.put("popl", Class.POP);

        // Return
        sMap.put("ret", Class.RET);
        
        // Leave
        sMap.put("leave", Class.LEAVE);
        
        // Extension
        sMap.put("cltd", Class.EXT);
        sMap.put("cwtl", Class.EXT);

        // System
        sMap.put("cpuid", Class.CPUID);
        sMap.put("hlt", Class.HALT);
        sMap.put("int", Class.INT);

        // Nop
        sMap.put("nop", Class.NOP);

    }

    private Class mClass;
    // FIXME: This field may cause memory consumption issue. Maybe we should
    // produce raw
    // opcode strings via a factory way.
    private String mCode;
    private boolean mIsLocked;

    public Opcode(String rawcode) {
        
        if (rawcode.startsWith("lock ")) {
            mIsLocked = true;
            rawcode = rawcode.substring(5);
        } else {
            mIsLocked = false;
        }
        mCode = rawcode;
        mClass = sMap.get(rawcode);
        if (mClass == null)
            mClass = Class.UD;
    }

    public String getRawOpcode() {
        return mCode;
    }
    
    public void setLocked(boolean isLocked) {
        mIsLocked = isLocked;
    }
    
    public boolean isLocked() {
        return mIsLocked;
    }
    
    public void setRawOpcode(String newRawOpcode) {
        mCode = newRawOpcode;
    }

    public Class getOpcodeClass() {
        return mClass;
    }

    public static Class query(String key) {
        Class ret = sMap.get(key);
        if (ret == null)
            throw new IllegalArgumentException("Unknown opcode " + key);
        else
            return ret;
    }
}
