package edu.psu.ist.plato.kaiming.x86;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.psu.ist.plato.kaiming.Machine;
import edu.psu.ist.plato.kaiming.Machine.Arch;
import edu.psu.ist.plato.kaiming.MachRegister;
import edu.psu.ist.plato.kaiming.exception.UnsupportedLanguageException;

public class Register extends MachRegister implements Operand  {
    
    public enum Id {
        EAX, EBX, ECX, EDX, ESP, EBP, ESI, EDI,
        AX, BX, CX, DX, SP, BP, SI, DI,
        AH, AL, BH, BL, CH, CL, DH, DL,
        FS, GS, CS, SS, DS, ES,
        EIP, EIZ,
    }
    
    private final static Map<String, Id> sNameMap = 
            new HashMap<String, Id>();
    
    static {
        sNameMap.put("eax", Id.EAX);
        sNameMap.put("ebx", Id.EBX);
        sNameMap.put("ecx", Id.ECX);
        sNameMap.put("edx", Id.EDX);
        sNameMap.put("esp", Id.ESP);
        sNameMap.put("ebp", Id.EBP);
        sNameMap.put("esi", Id.ESI);
        sNameMap.put("edi", Id.EDI);
        sNameMap.put("ax", Id.AX);
        sNameMap.put("bx", Id.BX);
        sNameMap.put("cx", Id.CX);
        sNameMap.put("dx", Id.DX);
        sNameMap.put("sp", Id.SP);
        sNameMap.put("bp", Id.BP);
        sNameMap.put("si", Id.SI);
        sNameMap.put("di", Id.DI);
        sNameMap.put("ah", Id.AH);
        sNameMap.put("al", Id.AL);
        sNameMap.put("bh", Id.BH);
        sNameMap.put("bl", Id.BL);
        sNameMap.put("ch", Id.CH);
        sNameMap.put("cl", Id.CL);
        sNameMap.put("dh", Id.DH);
        sNameMap.put("dl", Id.DL);
        sNameMap.put("fs", Id.FS);
        sNameMap.put("gs", Id.GS);
        sNameMap.put("cs", Id.CS);
        sNameMap.put("ss", Id.SS);
        sNameMap.put("ds", Id.DS);
        sNameMap.put("es", Id.ES);
        sNameMap.put("eip", Id.EIP);
        sNameMap.put("eiz", Id.EIZ);
    }
    
    private static final Set<Register.Id> s32BitGeneralRegs; 
            
    
    static {
        Set<Register.Id> s = new HashSet<Register.Id>(8);
        s.add(Id.EAX);
        s.add(Id.EBX);
        s.add(Id.ECX);
        s.add(Id.EDX);
        s.add(Id.ESI);
        s.add(Id.EDI);
        s.add(Id.ESP);
        s.add(Id.EBP);
        
        s32BitGeneralRegs = Collections.unmodifiableSet(s);
    }
    
    public final Id id;
    private final int mSize;

    private static final Register eax = new Register(Id.EAX, 32);
    private static final Register ebx = new Register(Id.EBX, 32);
    private static final Register ecx = new Register(Id.ECX, 32);
    private static final Register edx = new Register(Id.EDX, 32);
    private static final Register esp = new Register(Id.ESP, 32);
    private static final Register ebp = new Register(Id.EBP, 32);
    private static final Register esi = new Register(Id.ESI, 32);
    private static final Register edi = new Register(Id.EDI, 32);
    private static final Register ax = new Register(Id.AX, 16);
    private static final Register bx = new Register(Id.BX, 16);
    private static final Register cx = new Register(Id.CX, 16);
    private static final Register dx = new Register(Id.DX, 16);
    private static final Register sp = new Register(Id.SP, 16);
    private static final Register bp = new Register(Id.BP, 16);
    private static final Register si = new Register(Id.SI, 16);
    private static final Register di = new Register(Id.DI, 16);
    private static final Register ah = new Register(Id.AH, 8);
    private static final Register al = new Register(Id.AL, 8);
    private static final Register bh = new Register(Id.BH, 8);
    private static final Register bl = new Register(Id.BL, 8);
    private static final Register ch = new Register(Id.CH, 8);
    private static final Register cl = new Register(Id.CL, 8);
    private static final Register dh = new Register(Id.DH, 8);
    private static final Register dl = new Register(Id.DL, 8);
    private static final Register gs = new Register(Id.GS, 16);
    private static final Register fs = new Register(Id.FS, 16);
    private static final Register cs = new Register(Id.CS, 16);
    private static final Register ds = new Register(Id.DS, 16);
    private static final Register ss = new Register(Id.SS, 16);
    private static final Register es = new Register(Id.ES, 16);
    private static final Register eip = new Register(Id.EIP, 32);
    private static final Register eiz = new Register(Id.EIZ, 32);

    private Register(Id id, int size) {
        this.id = id;
        mSize = size;
    }
    
    public static Register getRegister(Id id) {
        switch (id) {
        	case EAX: return eax;
        	case EBX: return ebx;
        	case ECX: return ecx;
        	case EDX: return edx;
        	case ESP: return esp;
        	case EBP: return ebp;
        	case ESI: return esi;
        	case EDI: return edi;
        	case AX: return ax;
        	case BX: return bx;
        	case CX: return cx;
        	case DX: return dx;
        	case SP: return sp;
        	case BP: return bp;
        	case SI: return si;
        	case DI: return di;
        	case AH: return ah;
        	case AL: return al;
        	case BH: return bh;
        	case BL: return bl;
        	case CH: return ch;
        	case CL: return cl;
        	case DH: return dh;
        	case DL: return dl;
        	case GS: return gs;
        	case FS: return fs;
        	case CS: return cs;
        	case DS: return ds;
        	case SS: return ss;
        	case ES: return es;
        	case EIP: return eip;
        	case EIZ: return eiz;
        	default: throw new IllegalArgumentException("Unkown register");
        }
    }
    
    public static Register get(String name) {
        Id id = sNameMap.get(name);
        if (id == null)
            throw new UnsupportedLanguageException("Unknown register name: " + name);
        return getRegister(sNameMap.get(name));
    }
    
    public int sizeInBits() {
        return mSize;
    }
    
    public boolean isSegmentRegister() {
        switch (id) {
            case GS:
            case FS:
            case CS:
            case DS:
            case SS:
            case ES:
                return true;
            default:
                return false;
        }
    }
    
    public Register containingRegister() {
        switch (id) {
            case AX: 
            case AH: 
            case AL:
                return eax;
            case BX:
            case BH:
            case BL:
                return ebx;
            case CX: 
            case CH: 
            case CL:
                return ecx;
            case DX:
            case DH:
            case DL:
                return edx;
            case SP:
                return esp;
            case BP:
                return ebp;
            case SI:
                return esi;
            case DI:
                return edi;
            default:
                return this;
        }
    }
    
    public boolean isPseudoRegister() {
        switch (id) {
            case EIZ:
                return true;
            default:
                return false;
        }
    }
    
    public static Set<Register.Id> general32BitRegs () {
        return s32BitGeneralRegs;
    }

    @Override
    public String name() {
        return id.name().toLowerCase();
    }

    @Override
    public Arch arch() {
        return Machine.Arch.X86;
    }

    @Override
    public Set<MachRegister> subsumedRegisters() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isRegister() {
        return true;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isImmeidate() {
        return false;
    }

    @Override
    public Type type() {
        return Type.REGISTER;
    }

    @Override
    public Immediate asImmediate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Memory asMemory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Register asRegister() {
        return this;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that)
            return true;
        if (that instanceof Register) {
            return id == ((Register)that).id;
        }
        return false;
    }
    
    @Override
    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printOpRegister(this);
        p.close();
        return baos.toString();
    }
}
