package edu.psu.ist.plato.kaiming.ir;


import edu.psu.ist.plato.kaiming.Entry;

public class SetFlagStmt extends DefStmt {

    public SetFlagStmt(Entry inst, Flg flg, Expr expr) {
        super(Kind.SETF, inst, flg, new Expr[] {expr});
    }
    
}
