package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.Entry;

public class SelectStmt extends DefStmt {

    protected SelectStmt(Entry inst, Lval lval, Expr cond, Expr e1, Expr e2) {
        super(Kind.SELECT, inst, lval, new Expr[] { cond, e1, e2 });
    }

}
