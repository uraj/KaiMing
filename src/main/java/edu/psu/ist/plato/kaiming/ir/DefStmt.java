package edu.psu.ist.plato.kaiming.ir;

import java.util.HashSet;
import java.util.Set;

import edu.psu.ist.plato.kaiming.Entry;

abstract public class DefStmt extends Stmt {
	
	private Set<Stmt> mDUChain;
	private Lval mLval;
	
	protected DefStmt(Kind kind, Entry inst, Lval lval, Expr[] usedExpr) {
		super(kind, inst, usedExpr);
		mLval = lval;
		mDUChain = new HashSet<Stmt>();
	}
	
	public final Lval definedLval() {
	    return mLval;
	}
		
	final public Set<Stmt> defUseChain() {
		return mDUChain;
	}
	
	final public void updateDefUseMap(Set<Stmt> stmts) {
		mDUChain = stmts;
	}
	
	final public boolean addToDefUseChain(Stmt stmt) {
		return mDUChain.add(stmt);
	}
	
	final public boolean removeFromDefUseChain(Stmt stmt) {
		return mDUChain.remove(stmt);
	}
	
    /**
     * A special DefStmt object that indicates the initial definition
     * of all Lval values in a Context.
     */
    static final public DefStmt EXTERNAL = new DefStmt(null, null, null, new Expr[] {}) {};
    
    final public boolean isExternal() {
        return this == EXTERNAL;
    }
}
