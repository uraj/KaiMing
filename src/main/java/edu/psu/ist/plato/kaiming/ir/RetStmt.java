package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.Entry;

public class RetStmt extends Stmt {
    private static final Expr[] sEmpty = new Expr[] {};
    
    public RetStmt(Entry inst) {
        super(Kind.RET, inst, sEmpty);
    }
    
    public RetStmt(Entry inst, Expr target) {
        super(Kind.RET, inst, new Expr[] { target });
    }
}
