package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.Set;

import edu.psu.ist.plato.kaiming.x86.Instruction;

abstract public class DefStmt extends Stmt {
	
	private Set<Stmt> mDUChain;
	
	protected DefStmt(Kind kind, Instruction inst, Expr[] usedExpr) {
		super(kind, inst, usedExpr);
	}
	
	abstract public Lval definedLval();
		
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
    static public DefStmt External = new DefStmt(null, null, new Expr[] {}) {
        @Override
        public Lval definedLval() {
            return null;
        }
    };
    
    final public boolean isExternal() {
        return this == External;
    }
}
