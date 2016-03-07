package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

abstract public class Stmt {
    protected Instruction mInst;
    
    protected Stmt(Instruction inst){
        mInst = inst;
    }
    
    public Instruction getStatement() {
        return mInst;
    }
}
