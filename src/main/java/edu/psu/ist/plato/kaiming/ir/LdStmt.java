package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

public class LdStmt extends DefStmt {

	public LdStmt(Instruction inst, Expr addr, Lval content) {
		super(Kind.LD, inst, content, new Expr[] { addr });
	}
	
	public Expr loadFrom() {
		return usedExpr(0);
	}

}
