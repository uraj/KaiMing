package edu.psu.ist.plato.kaiming.arm64;

import java.util.Iterator;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.util.ArrayIterator;
import edu.psu.ist.plato.kaiming.util.Assert;

public class Instruction extends Entry implements Iterable<Operand> {
    
    public enum Kind {
        BIN_ARITHN,
        UN_ARITH,
        LOAD,
        LOAD_PAIR,
        STORE,
        STORE_PAIR,
        BRANCH,
        PUSH,
        POP,
        NOP,
        MOVE,
        COMPARE,
        SELECT,
        BITFIELD_MOVE,
    }
    
    private Kind mKind;
    private long mAddr;
    private Opcode mOpcode;
    private Operand[] mOperands;
    
    protected Instruction(Kind kind, long addr, Opcode op, Operand[] operands) {
        mKind = kind;
        mAddr = addr;
        mOpcode = op;
        mOperands = operands;
    }
    
    public long addr() {
        return mAddr;
    }
    
    public Kind kind() {
        return mKind;
    }
    
    public Opcode opcode() {
        return mOpcode;
    }
    
    public final Operand operand(int index) {
        return mOperands[index];
    }
    
    protected final void setOperand(int index, Operand op) {
        mOperands[index] = op;
    }
    
    @Override
    public Iterator<Operand> iterator() {
        return new ArrayIterator<Operand>(mOperands);
    }

    @Override
    public long index() {
        return mAddr;
    }
    
    public int fillLabelInformation(Label l) {
        l.setAddr(mAddr);
        return 0;
    }
    
    public static Instruction create(long addr, Opcode opcode, Operand[] oplist, Condition cond) {
        Instruction ret = null;
        cond = cond == null ? Condition.AL : cond; 
        switch (opcode.mnemonic()) {
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case ASR:
            case LSL:
            case LSR:
            case ORR:
            case ORN:
            case AND:
                Assert.verify(oplist.length == 3 ||  oplist.length == 2);
                Assert.verify(oplist[0].isRegister());
                if (oplist.length == 3)
                    ret = new BinaryArithInst(addr, opcode, oplist[0].asRegister(), oplist[1], oplist[2]);
                else if (oplist.length == 2)
                    ret = new BinaryArithInst(addr, opcode, oplist[0].asRegister(),
                            oplist[0].asRegister(), oplist[1]);
                break;
            case ADR:
            case NEG:
                break;
            case LDR:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && oplist[1].isMemory());
                ret = new LoadInst(addr, opcode, oplist[0].asRegister(), oplist[1].asMemory());
                break;
            case LDP:
                Assert.verify(oplist.length == 3);
                Assert.verify(oplist[0].isRegister() && oplist[1].isRegister() && oplist[2].isMemory());
                ret = new LoadPairInst(addr, opcode, oplist[0].asRegister(),
                        oplist[1].asRegister(), oplist[2].asMemory());
                break;
            case STR:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister());
                Assert.verify(oplist[1].isMemory());
                ret = new StoreInst(addr, opcode, oplist[0].asRegister(), oplist[1].asMemory());
                break;
            case STP:
                Assert.verify(oplist.length == 3);
                Assert.verify(oplist[0].isRegister() &&
                        oplist[1].isRegister() && oplist[2].isMemory());
                ret = new StorePairInst(addr, opcode, oplist[0].asRegister(),
                        oplist[1].asRegister(), oplist[2].asMemory());
                break;
            case CMP:
            case CMN:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && oplist[1].isRegister());
                ret = new CompareInst(addr, opcode, 
                        oplist[0].asRegister(), oplist[1].asRegister());
                break;
            case CSEL:
            case CSINC:
                Assert.verify(oplist.length == 3);
                Assert.verify(oplist[0].isRegister() && 
                        oplist[1].isRegister() && oplist[2].isRegister());
                ret = new SelectInst(addr, opcode, oplist[0].asRegister(),
                        oplist[1].asRegister(), oplist[2].asRegister(), cond);
                break;
            case CINC:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && oplist[1].isRegister());
                ret = new SelectInst(addr, new Opcode("CSINC"), oplist[0].asRegister(),
                        oplist[1].asRegister(), oplist[1].asRegister(), cond);
                break;
            case CSET:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && oplist[1].isRegister());
                ret = new SelectInst(addr, new Opcode("CSINC"), oplist[0].asRegister(),
                        oplist[0].asRegister(), oplist[1].asRegister(), cond);
                break;
            case MOV:
            case MOVK:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister());
                ret = new MoveInst(addr, opcode, oplist[0].asRegister(), oplist[1]);
                break;
            case EXT:
                Assert.verify(oplist.length == 2);
                Assert.verify(oplist[0].isRegister() && oplist[1].isRegister());
                ret = new BitfieldMoveInst(addr, opcode,
                        oplist[0].asRegister(), oplist[1].asRegister());
                break;
            case B:
            case BL:
                Assert.verify(oplist.length <= 1);
                if (oplist.length == 1) {
                    Assert.verify(oplist[0].isMemory() || oplist[0].isRegister());
                    ret = new BranchInst(addr, opcode, oplist[0]);
                } else {
                    ret = new BranchInst(addr, opcode);
                }
                break;
            case NOP:
                Assert.verify(oplist.length == 0);
                ret = new NopInst(addr);
                break;
        }
        Assert.verify(ret != null);
        return ret;
    }
}
