package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

public class StStmt extends Stmt {

	private Expr mAddr;
	private Expr mContent;
	
	protected StStmt(Instruction inst, Expr addr, Expr content) {
		super(inst);
		mAddr = addr;
		mContent = content;
	}
	
	public Expr getAddr() {
		return mAddr;
	}
	
	public Expr getContent() {
		return mContent;
	}

}
