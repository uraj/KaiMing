package edu.psu.ist.plato.kaiming.x86;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Label;

// TODO: We should re-implement this once we have the  
// visitor infrastructure ready.
public class Printer extends PrintWriter {

    private boolean mIsParseMode;

    public Printer(OutputStream ps, boolean parsemode) {
        super(ps, true);
        mIsParseMode = parsemode;
    }

    public Printer(OutputStream ps) {
        super(ps, true);
        mIsParseMode = false;
    }
    
    private void printSignedHex(long value) {
        if (value < 0) {
            print(String.format("-0x%x", -value));
        } else {
            print(String.format("0x%x", value));
        }
    }

    public void printOpImmediate(Immediate imm) {
        print('$');
        printSignedHex(imm.getValue());
    }

    public static Printer out = new Printer(System.out);
    public static Printer err = new Printer(System.err);
    
    public void printOpMemory(Memory mem) {
        if (!mIsParseMode && mem.isRelocation()) {
            print(mem.asRelocation().label().name());
            return;
        }
        if (mem.baseRegister() != null
                && mem.baseRegister().isSegmentRegister()) {
            printOpRegister(mem.baseRegister());
            print(':');
            if (mem.offsetRegister() != null) {
                print('(');
                printOpRegister(mem.offsetRegister());
                print(')');
            } else
                printSignedHex(mem.displacement());
            return;
        }
        
        long disp = mem.displacement();
        printSignedHex(disp);
        if (mem.baseRegister() == null && mem.offsetRegister() == null)
            return;
        print('(');
        if (mem.baseRegister() != null
                && !mem.baseRegister().isSegmentRegister()) {
            printOpRegister(mem.baseRegister());
        }
        if (mem.offsetRegister() != null) {
            print(',');
            printOpRegister(mem.offsetRegister());
        }
        if (mem.multiplier() != 1) {
            print(',');
            print(mem.multiplier());
        }
        print(')');
    }

    public void printOpRegister(Register reg) {
        print('%');
        print(reg.id.name().toLowerCase());
    }

    public void printOperand(Operand op) {
        switch (op.type()) {
            case IMMEDIATE:
                printOpImmediate((Immediate) op);
                break;
            case MEMORY:
                printOpMemory((Memory) op);
                break;
            case REGISTER:
                printOpRegister((Register) op);
                break;
        }
    }

    public void printInstruction(Instruction i) {
        if (mIsParseMode)
            printSignedHex(i.addr());
        print('\t');
        print(i.opcode().rawOpcode());
        print('\t');
        if (i.isBranchInst()) {
            BranchInst bi = (BranchInst)i;
            if (bi.isIndirect())
                print('*');
        }
        Iterator<Operand> iter = i.iterator();
        if (iter.hasNext()) {
            while (true) {
                printOperand(iter.next());
                if (iter.hasNext())
                    print(", ");
                else
                    break;
            }
        }
    }

    public void printLabel(Label label) {
        if (mIsParseMode) {
            printSignedHex(label.addr());
            print('\t');
        }
        print(label.name());
    }

    public void printFunction(Function f) {
        printLabel(f.label());
        println(':');
        for (Instruction i : f.entries()) {
            printInstruction(i);
            println();
        }
    }
    
    public void printBasicBlock(BasicBlock<Instruction> bb) {
        if (!mIsParseMode) {
            printLabel(bb.label());
            println(':');
        }
        for (Entry e : bb) {
            printInstruction((Instruction)e);
            println();
        }
    }
    
    public void printCFG(CFG<Instruction> f) {
        if (mIsParseMode) {
            printLabel((Label)f.entryBlock().label());
            println(':');
        }
        for (BasicBlock<Instruction> bb : f) {
            printBasicBlock(bb);
        }
    }
}
