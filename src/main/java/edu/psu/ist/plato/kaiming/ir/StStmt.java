package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.Entry;

public class StStmt extends Stmt {

	public StStmt(Entry inst, Expr addr, Expr content) {
		super(Kind.ST, inst, new Expr[] { addr, content });
	}
	
	public Expr storeTo() {
		return usedExpr(0);
	}
	
	public Expr storedExpr() {
		return usedExpr(1);
	}

}
