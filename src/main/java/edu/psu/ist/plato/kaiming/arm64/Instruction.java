package edu.psu.ist.plato.kaiming.arm64;

import java.util.Iterator;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.util.ArrayIterator;

public class Instruction extends Entry implements Iterable<Operand> {
    
    public enum Kind {
        BIN_ARITHN,
        UN_ARITH,
        LOAD,
        STORE,
        BRANCH_LINK,
        BRANCH,
        RET,
        PUSH,
        POP,
        NOP,
        MOVE,
        COMPARE,
    }
    
    private Kind mKind;
    private long mAddr;
    private Opcode mOpcode;
    private Operand[] mOperands;
    private Condition mCond;
    
    protected Instruction(Kind kind, long addr, Opcode op, Operand[] operands, Condition cond) {
        mKind = kind;
        mAddr = addr;
        mOpcode = op;
    }
    
    public Kind kind() {
        return mKind;
    }
    
    public Opcode opcode() {
        return mOpcode;
    }
    
    public final Operand getOperand(int index) {
        return mOperands[index];
    }
    
    protected final void setOperand(int index, Operand op) {
        mOperands[index] = op;
    }
    
    public Condition cond() {
        return mCond;
    }

    @Override
    public Iterator<Operand> iterator() {
        return new ArrayIterator<Operand>(mOperands);
    }

    @Override
    public long index() {
        return mAddr;
    }
    
    public static Instruction create(long addr, Opcode opcode, Operand[] oplist, Condition cond) {
        return null;
    }

}
