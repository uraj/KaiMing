package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.Map;
import java.util.Set;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.util.Tuple;
import edu.psu.ist.plato.kaiming.BasicBlock;
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
    
    protected Map<Lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>>> mUDChain;
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
    
    public Set<Tuple<BasicBlock<Stmt>, DefStmt>> getDef(Lval lval) {
    	return mUDChain.get(lval);
    }
    
    public Set<Tuple<BasicBlock<Stmt>, DefStmt>>
    setDef(Lval lval, Set<Tuple<BasicBlock<Stmt>, DefStmt>> stmt) {
    	return mUDChain.put(lval, stmt);
    }
}
