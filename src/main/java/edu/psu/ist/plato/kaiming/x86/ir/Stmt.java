package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.x86.Instruction;


abstract public class Stmt extends Entry {
    
    protected Instruction mInst;
    protected long mIndex;
    
    protected Stmt(Instruction inst) {
        mInst = inst;
        mIndex = -1;
    }
    
    public Instruction getStatement() {
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
