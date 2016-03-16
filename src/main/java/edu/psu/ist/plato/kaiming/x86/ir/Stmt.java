package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.LinkedList;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Label;
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
    
    @Override
    public int fillLabelInformation(Label l) {
        return 0;
    }

    @Override
    public int fillLabelInformation(Label l, Entry e) {
        return 0;
    }
    
    public static LinkedList<Stmt> toIRStatements(Instruction inst) {
        LinkedList<Stmt> ret = new LinkedList<Stmt>();
        return ret;
    }
}
