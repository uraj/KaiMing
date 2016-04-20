package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    
    protected class LvalProbe extends Expr.Visitor {
        
        private Set<Lval> mLvals = new HashSet<Lval>(); 
        
        @Override
        protected boolean visitLval(Lval lval) {
            mLvals.add(lval);
            return true;
        }
        
        public Set<Lval> probedLvals() {
            return mLvals;
        }
    }
    
    protected final Instruction mInst;
    protected long mIndex;
    
    private Expr[] mUsedExpr;
    
    private Map<Lval, Set<DefStmt>> mUDChain;
    private final Kind mKind;
    
    protected Stmt(Kind kind, Instruction inst, Expr[] usedExpr) {
        mKind = kind;
        mInst = inst;
        mIndex = -1;
        mUsedExpr = usedExpr;
        mUDChain = new HashMap<Lval, Set<DefStmt>>();
        LvalProbe prob = new LvalProbe();
        for (Expr e : mUsedExpr) {
            prob.visit(e);
        }
        prob.probedLvals().remove(Reg.eip); // EIP is always self defined
        for (Lval lv : prob.probedLvals()) {
            mUDChain.put(lv, null);
        }
    }
    
    final public Kind kind() {
        return mKind;
    }
    
    final public Instruction hostInstruction() {
        return mInst;
    }
    
    @Override
    final public long index() {
        return mIndex;
    }
    
    final public void setIndex(long index) {
        mIndex = index;
    }
    
    final public Set<DefStmt> searchDefFor(Lval lval) {
    	return mUDChain.get(lval);
    }
    
    final public Set<DefStmt> updateDefFor(Lval lval, Set<DefStmt> stmt) {
    	return mUDChain.put(lval, stmt);
    }
    
    final public Set<Lval> usedLvals() {
        return mUDChain.keySet();
    }
    
    public Set<Expr> enumerateAllExpr() {
        class Enumerator extends Expr.Visitor {
            
            private Set<Expr> mExprs;
            
            public Enumerator(Set<Expr> init) {
                mExprs = init;
            }
            
            private boolean recordExpr(Expr e) {
                int size = mExprs.size();
                mExprs.add(e);
                return mExprs.size() != size;
            }
            
            @Override
            protected boolean visitUExpr(UExpr expr) {
                return recordExpr(expr);
            }
            
            public Set<Expr> enumeratedExprs() {
                return mExprs;
            }
        }
        
        Enumerator enumerator = new Enumerator(new HashSet<Expr>());
        for (Expr expr : mUsedExpr) {
            enumerator.visit(expr);
        }
        
        return enumerator.enumeratedExprs();
    }
}
