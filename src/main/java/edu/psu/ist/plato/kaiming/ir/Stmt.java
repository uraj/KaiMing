package edu.psu.ist.plato.kaiming.ir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Machine;

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
        SELECT,
    }
    
    protected class LvalProbe extends Expr.Visitor {
        
        private Set<Lval> mLvals = new HashSet<Lval>(); 
        
        @Override
        protected boolean visitReg(Reg lval) {
            mLvals.add(lval);
            return true;
        }
        
        @Override
        protected boolean visitVar(Var lval) {
            mLvals.add(lval);
            return true;
        }
        
        public Set<Lval> probedLvals() {
            return mLvals;
        }
    }
    
    protected final Entry mHost;
    protected long mIndex;
    
    private Expr[] mUsedExpr;
    
    private Map<Lval, Set<DefStmt>> mUDChain;
    private final Kind mKind;
    
    protected Stmt(Kind kind, Entry inst, Expr[] usedExpr) {
        mKind = kind;
        mHost = inst;
        mIndex = -1;
        mUsedExpr = usedExpr;
        mUDChain = new HashMap<Lval, Set<DefStmt>>();
        LvalProbe prob = new LvalProbe();
        for (Expr e : mUsedExpr) {
            prob.visit(e);
        }
        // FIXME: Program counter is self defined most of the time,
        // but certain architectures allow explicit assignment to
        // to PC register. How would this affect the design of out
        // UD analysis algorithm?
        //
        //prob.probedLvals().remove(Reg.eip);
        for (Lval lv : prob.probedLvals()) {
            mUDChain.put(lv, new HashSet<DefStmt>());
        }
    }
    
    final public Kind kind() {
        return mKind;
    }
    
    final public Entry hostEntry() {
        return mHost;
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
    
    protected final Expr usedExpr(int idx) {
        return mUsedExpr[idx];
    }
    
    public Set<Expr> enumerateRval() {
        class Enumerator extends Expr.Visitor {
            
            private Set<Expr> mExprs;
            
            public Enumerator(Set<Expr> init) {
                mExprs = init;
            }
            
            @Override
            protected boolean prologue(Expr expr) {
                return mExprs.add(expr);
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
    
    @Override
    public final String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Printer p = new Printer(new PrintStream(baos));
        p.printStmt(this);
        p.close();
        return baos.toString();
    }
    
    @Override
    public Machine machine() {
        throw new UnsupportedOperationException();
    }
}
