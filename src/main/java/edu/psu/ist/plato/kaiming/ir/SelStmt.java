package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.Entry;

public class SelStmt extends DefStmt {

    public SelStmt(Entry inst, Lval lval, Expr cond, Expr e1, Expr e2) {
        super(Kind.SELECT, inst, lval, new Expr[] { cond, e1, e2 });
    }
    
    public Expr condition() {
        return usedExpr(0);
    }
    
    public Expr truevalue() {
        return usedExpr(1);
    }
    
    public Expr falsevalue() {
        return usedExpr(2);
    }

}
