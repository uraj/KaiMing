package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.util.Tuple;

public class AssignStmt extends DefStmt {

    private Tuple<Integer, Integer> mUpdateRange;

    public AssignStmt(Entry inst, Lval lval, Expr expr) {
        super(Kind.ASSIGN, inst, lval, new Expr[] { expr });
        mUpdateRange = new Tuple<Integer, Integer>(0, inst.machine().wordSizeInBits());
    }
    
    // [rangeInsig, rangeSig)
    public AssignStmt(Entry inst, Lval lval, Expr expr, int rangeInsig, int rangeSig) {
        super(Kind.ASSIGN, inst, lval, new Expr[] { expr });
        mUpdateRange = new Tuple<Integer, Integer>(rangeInsig, rangeSig);
    }
    
    public Expr usedRval() {
        return usedExpr(0);
    }

    public Tuple<Integer, Integer> rangeOfAssignment() {
        return mUpdateRange;
    }
}
