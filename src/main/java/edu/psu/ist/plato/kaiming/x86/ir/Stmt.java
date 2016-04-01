package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.Map;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.x86.Instruction;


abstract public class Stmt extends Entry {
    
    protected final Instruction mInst;
    protected long mIndex;
    
    protected Map<Lval, DefStmt> mUDChain;
    
    protected Stmt(Instruction inst) {
        mInst = inst;
        mIndex = -1;
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
    
    public DefStmt getDef(Lval lval) {
    	return mUDChain.get(lval);
    }
    
    public DefStmt setDef(Lval lval, DefStmt stmt) {
    	return mUDChain.put(lval, stmt);
    }
}
