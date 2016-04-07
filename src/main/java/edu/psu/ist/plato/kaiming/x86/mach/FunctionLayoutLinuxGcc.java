package edu.psu.ist.plato.kaiming.x86.mach;

import java.util.List;

import edu.psu.ist.plato.kaiming.util.Tuple;
import edu.psu.ist.plato.kaiming.x86.Function;
import edu.psu.ist.plato.kaiming.x86.Immediate;
import edu.psu.ist.plato.kaiming.x86.Instruction;
import edu.psu.ist.plato.kaiming.x86.Memory;
import edu.psu.ist.plato.kaiming.x86.Opcode;
import edu.psu.ist.plato.kaiming.x86.Operand;
import edu.psu.ist.plato.kaiming.x86.Register;

public class FunctionLayoutLinuxGcc {
    public enum Addressing {
        ESP_ADDRESSING,
        EBP_ADDRESSING
    }
    
    private int mNumArgs;
    private Addressing mAddressing;
    private int mStackFrameSize;
    private int mNumPushes;
    public FunctionLayoutLinuxGcc(Function func) {
        mAddressing = guessAddressing(func);
        Tuple<Integer, Integer> tmp = guessStackFrameLayout(func);
        mNumPushes = tmp.first;
        mStackFrameSize = tmp.second;
        mNumArgs = guessArgumentNumber(func, mAddressing);
    }

    public int getArgumentNumber() {
        return mNumArgs;
    }
    
    public Addressing getAddressing() {
        return mAddressing;
    }
    
    // The heuristic is that if a function saves esp in ebp, then it 
    // tends to use ebp to address its local variables and parameters. 
    private Addressing guessAddressing(Function func) {
        for (Instruction i : func.entries()) {
            if (i.isMoveInst()) {
                Operand o0, o1;
                o0 = i.operand(0);
                o1 = i.operand(1);
                if (o0.isRegister() && o1.isRegister()) {
                    Register from = o0.asRegister();
                    Register to = o1.asRegister();
                    if (from.id == Register.Id.ESP && to.id == Register.Id.EBP) {
                        return Addressing.EBP_ADDRESSING;
                    }
                }
            }
        }
        return Addressing.ESP_ADDRESSING;
    }
    
    private Tuple<Integer, Integer> guessStackFrameLayout(Function func) {
        int framesize = 0;
        int numPushes = 0;
        for (Instruction i : func.entries()) {
            if (i.isPushInst())
                ++numPushes;
            else if (i.opcode().opcodeClass() == Opcode.Class.SUB) {
                Operand o0, o1;
                o0 = i.operand(0);
                o1 = i.operand(1);
                if (o1.isRegister() && o1.asRegister().id == Register.Id.ESP && o0.isImmeidate()) {
                    Immediate imm = o0.asImmediate();
                    framesize = (int)imm.getValue();
                    break;
                }
            }
        }
        return new Tuple<Integer, Integer>(numPushes, framesize);
    }
    
    private int guessArgumentNumber(Function func, Addressing addr) {
        int ret = -1;
        List<Instruction> insts = func.entries();
        if (addr == Addressing.EBP_ADDRESSING) {
            for (Instruction i : insts) {
                for (Operand o : i.operands()) {
                    if (o.isMemory()) {
                        Memory m = o.asMemory();
                        if (m.offsetRegister() == null && 
                                m.baseRegister().id == Register.Id.EBP) {
                            long disp = m.displacement();
                            if (disp > Integer.MAX_VALUE)
                                continue;
                            int num = (int)(disp / 4 - 1);
                            ret = Math.max(ret, num);
                        }
                    }
                }
            }
        } else if (addr == Addressing.ESP_ADDRESSING) {
            int startOfArgs = mStackFrameSize + 4 * (mNumPushes + 1);
            for (Instruction i : insts) {
                for (Operand o : i.operands()) {
                    if (o.isMemory()) {
                        Memory m = o.asMemory();
                        if (m.offsetRegister() == null && 
                                m.baseRegister().id == Register.Id.EBP) {
                            long disp = m.displacement();
                            if (disp < startOfArgs && disp > Integer.MAX_VALUE)
                                continue;
                            int num = (int)(disp / 4);
                            ret = Math.max(ret, num);
                        }
                    }
                }
            }
        }
        return ret;
    }
}
