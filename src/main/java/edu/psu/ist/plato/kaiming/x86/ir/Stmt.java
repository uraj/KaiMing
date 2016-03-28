package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.x86.Instruction;


abstract public class Stmt extends Entry {
    
    public enum Kind {
        ASSIGN,
        CALL,
        CMP,
        JMP,
        LD,
        SETF,
        ST,
        RET,
    }
    
    protected final Instruction mInst;
    protected long mIndex;
    private final Kind mKind;
    
    protected Stmt(Kind kind, Instruction inst) {
        mKind = kind;
        mInst = inst;
        mIndex = -1;
    }
    
    public final Kind kind() {
        return mKind;
    }
    
    public Instruction getHostingInstruction() {
        return mInst;
    }
    
    @Override
    public long getIndex() {
        return mIndex;
    }
    
    public void setIndex(long index) {
        mIndex = index;
    }
}
