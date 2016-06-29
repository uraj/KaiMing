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
    
    protected final Entry mHost;
    protected long mIndex;
    
    private Expr[] mUsedExpr;
    
    private Map<Lval, Set<DefStmt>> mUDChain;
    private final Kind mKind;
    
    public String comment;
    
    protected Stmt(Kind kind, Entry inst, Expr[] usedExpr) {
        mKind = kind;
        mHost = inst;
        mIndex = -1;
        mUsedExpr = usedExpr;
        initializeUDChain();
    }
    
    private void initializeUDChain() {
        mUDChain = new HashMap<Lval, Set<DefStmt>>();
        Expr.LvalProbe prob = new Expr.LvalProbe();
        for (Expr e : mUsedExpr) {
            prob.visit(e);
        }
        Set<Lval> probedLvals = prob.probedLvals();
        for (Lval lv : probedLvals) {
            mUDChain.put(lv, new HashSet<DefStmt>());
        }
    }
    
    final public Kind kind() {
        return mKind;
    }
    
    final public Entry hostEntry() {
        return mHost;
    }
    
    final public long hostIndex() {
        return mHost.index();
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

    final public int numOfUsedExpr() {
        return mUsedExpr.length;
    }
    
    protected final Expr usedExpr(int idx) {
        return mUsedExpr[idx];
    }
    
    final void setUsedExpr(int idx, Expr n) {
        mUsedExpr[idx] = n;
        initializeUDChain();
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
