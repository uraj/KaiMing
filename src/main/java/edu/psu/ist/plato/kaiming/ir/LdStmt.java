package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.Entry;

public class LdStmt extends DefStmt {

	public LdStmt(Entry inst, Lval content, Expr addr) {
		super(Kind.LD, inst, content, new Expr[] { addr });
	}
	
	public Expr loadFrom() {
		return usedExpr(1);
	}

}
