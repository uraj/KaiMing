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
        if (!mIsParseMode && mem instanceof Relocation) {
            print(((Relocation) mem).getLabel().getName());
            return;
        }
        if (mem.getBaseRegister() != null
                && mem.getBaseRegister().isSegmentRegister()) {
            printOpRegister(mem.getBaseRegister());
            print(':');
            if (mem.getOffsetRegister() != null) {
                print('(');
                printOpRegister(mem.getOffsetRegister());
                print(')');
            } else
                printSignedHex(mem.getDisplacement());
            return;
        }
        
        long disp = mem.getDisplacement();
        printSignedHex(disp);
        if (mem.getBaseRegister() == null && mem.getOffsetRegister() == null)
            return;
        print('(');
        if (mem.getBaseRegister() != null
                && !mem.getBaseRegister().isSegmentRegister()) {
            printOpRegister(mem.getBaseRegister());
        }
        if (mem.getOffsetRegister() != null) {
            print(',');
            printOpRegister(mem.getOffsetRegister());
        }
        if (mem.getMultiplier() != 1) {
            print(',');
            print(mem.getMultiplier());
        }
        print(')');
    }

    public void printOpRegister(Register reg) {
        print('%');
        print(reg.id.name().toLowerCase());
    }

    public void printOperand(Operand op) {
        switch (op.getType()) {
            case Immediate:
                printOpImmediate((Immediate) op);
                break;
            case Memory:
                printOpMemory((Memory) op);
                break;
            case Register:
                printOpRegister((Register) op);
                break;
        }
    }

    public void printInstruction(Instruction i) {
        if (mIsParseMode)
            printSignedHex(i.getAddr());
        print('\t');
        print(i.getOpcode().getRawOpcode());
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
            printSignedHex(label.getAddr());
            print('\t');
        }
        print(label.getName());
    }

    public void printFunction(Function f) {
        printLabel(f.getLabel());
        println(':');
        for (Instruction i : f.getInstructions()) {
            printInstruction(i);
            println();
        }
    }
    
    public void printBasicBlock(BasicBlock<Instruction> bb) {
        if (!mIsParseMode) {
            printLabel(bb.getLabel());
            println(':');
        }
        for (Entry e : bb) {
            printInstruction((Instruction)e);
            println();
        }
    }
    
    public void printCFG(CFG<Instruction> f) {
        if (mIsParseMode) {
            printLabel((Label)f.getEntryBlock().getLabel());
            println(':');
        }
        for (BasicBlock<Instruction> bb : f) {
            printBasicBlock(bb);
        }
    }
}
