package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

public class LdStmt extends DefStmt {

	private Expr mAddr;
	private Lval mContent;
	
	protected LdStmt(Instruction inst, Expr addr, Lval content) {
		super(Kind.LD, inst, new Expr[] { addr });
		mAddr = addr;
		mContent = content;
	}
	
	public Expr getAddr() {
		return mAddr;
	}

	@Override
	public Lval getDefinedLval() {
		return mContent;
	}

}
