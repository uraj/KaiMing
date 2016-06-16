package edu.psu.ist.plato.kaiming.ir;


import edu.psu.ist.plato.kaiming.util.Assert;
import edu.psu.ist.plato.kaiming.Entry;


public class SetFlagStmt extends DefStmt {

    
    public SetFlagStmt(Entry inst) {
        super(Kind.SETF, inst, new Expr[] {});
    }

    @Override
    public Lval definedLval() {
        Assert.unreachable();
        return null;
    }
}
