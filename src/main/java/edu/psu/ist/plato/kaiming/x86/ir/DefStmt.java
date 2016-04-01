package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.Set;

import edu.psu.ist.plato.kaiming.x86.Instruction;

abstract public class DefStmt extends Stmt {
	
	protected Set<Stmt> mDUChain;
	
	public DefStmt(Kind kind, Instruction inst) {
		super(kind, inst);
	}
	
	abstract public Lval getLval();
	
	final public Set<Stmt> getDefUseChain() {
		return mDUChain;
	}
	
	final public Set<Stmt> updateDefUseMap(Set<Stmt> stmts) {
		return mDUChain = stmts;
	}
	
	final public boolean addToDefUseChain(Stmt stmt) {
		return mDUChain.add(stmt);
	}
	
	final public boolean removeFromDefUseChain(Stmt stmt) {
		return mDUChain.remove(stmt);
	}
}
