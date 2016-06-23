package edu.psu.ist.plato.kaiming.arm64;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.arm64.LoadStoreInst.AddressingMode;
import edu.psu.ist.plato.kaiming.util.Assert;

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
        print('#');
        printSignedHex(imm.value());
    }
    
    public void printOpRegister(Register reg) {
        print(reg.id.name());
        if (reg.isShifted()) {
            print(", ");
            Register.Shift shift = reg.shift();
            print(shift.type().name());
            print(", ");
            print(shift.value());
        }
    }
    
    public void printOpMemory(Memory mem) {
        print('[');
        Register base = mem.base();
        if (base != null) {
            printOpRegister(base);
        }
        Memory.Offset off = mem.offset();
        if (off != null) {
            print(", ");
            if (off.isImmediateOffset()) {
                print('#');
                printSignedHex(off.asImmOff().value);
            } else {
                printOpRegister(off.asRegOff().value);
            }
        }
        print(']');
    }
    
    public void printOperand(Operand o) {
        switch (o.type()) {
            case IMMEDIATE:
                printOpImmediate(o.asImmediate());
                break;
            case REGISTER:
                printOpRegister(o.asRegister());
                break;
            case MEMORY:
                printOpMemory(o.asMemory());
                break;
        }
    }
    
    public void printInstruction(Instruction i) {
        if (mIsParseMode) {
            printSignedHex(i.addr());
            print('\t');
        }
        print(i.opcode().rawOpcode());
        print('\t');
        if (i instanceof BranchInst) {
            BranchInst b = (BranchInst)i;
            if (!b.isReturn()) {
                Operand target = b.target();
                if (target.isRegister()) {
                    printOpRegister(target.asRegister());
                } else if (target.isMemory()) {
                    if (target instanceof Relocation) {
                        print(((Relocation)target).targetBlock().label());
                    } else
                        printSignedHex(target.asMemory().offset().asImmOff().value);
                } else {
                    Assert.unreachable();
                }
            }
        } else {
            Iterator<Operand> iter = i.iterator();
            int indexingOperand = -1;
            LoadStoreInst.AddressingMode mode = AddressingMode.REGULAR;
            if (i instanceof LoadStoreInst) {
                mode = ((LoadStoreInst)i).addressingMode();
                if (mode != AddressingMode.REGULAR)
                    indexingOperand = ((LoadStoreInst)i).indexingOperand(); 
            }
            
            int round = 0;
            if (iter.hasNext()) {
                while (true) {
                    if (round == indexingOperand) {
                        switch (mode) {
                            case PRE_INDEX:
                                printOperand(iter.next());
                                print('!');
                                break;
                            case POST_INDEX: {
                                Memory next = iter.next().asMemory();
                                printOperand(next.base());
                                print(", #");
                                printSignedHex(next.offset().asImmOff().value);
                                break;
                            }
                            case REGULAR:
                                Assert.unreachable();
                        }
                    } else 
                        printOperand(iter.next());
                    if (iter.hasNext())
                        print(", ");
                    else
                        break;
                    ++round;
                }
            }
        }
        if (i instanceof CondInstruction) {
            print(", ");
            print(((CondInstruction)i).condition());
        }
    }
    
    public void printLabel(Label label) {
        if (mIsParseMode) {
            printSignedHex(label.addr());
            print('\t');
        }
        print(label.name());
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
    
    public void printBasicBlock(BasicBlock<Instruction> bb) {
        if (!mIsParseMode) {
            printLabel(bb.label());
            println(':');
        }
        for (Instruction i : bb) {
            printInstruction(i);
            println();
        }
    }
    
    public void printFunction(Function f) {
        printLabel(f.label());
        println(':');
        for (Instruction i : f.entries()) {
            printInstruction(i);
            println();
        }
    }
}