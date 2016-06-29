package edu.psu.ist.plato.kaiming.ir;

import java.util.HashSet;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.util.Tuple;

public class AssignStmt extends DefStmt {

    private Tuple<Integer, Integer> mUpdateRange;

    public AssignStmt(Entry inst, Lval lval, Expr expr) {
        super(Kind.ASSIGN, inst, lval, new Expr[] { expr });
        mUpdateRange = new Tuple<Integer, Integer>(0, lval.sizeInBits());
    }
    
    // [rangeInsig, rangeSig)
    public AssignStmt(Entry inst, Lval lval, Expr expr, int rangeInsig, int rangeSig) {
        super(Kind.ASSIGN, inst, lval, new Expr[] { expr });
        int maxsize = lval.sizeInBits();
        mUpdateRange = new Tuple<Integer, Integer>(rangeInsig,
                rangeSig > maxsize ? maxsize : rangeSig);
        if (mUpdateRange.first != 0 || mUpdateRange.second != lval.sizeInBits()) {
            updateDefFor(lval, new HashSet<DefStmt>());
        }
            
    }
    
    public Expr usedRval() {
        return usedExpr(0);
    }

    public Tuple<Integer, Integer> rangeOfAssignment() {
        return new Tuple<>(mUpdateRange);
    }
    
    public boolean isPartialAssignment() {
        return mUpdateRange.first != 0 || 
                mUpdateRange.second != definedLval().sizeInBits();
    }
}
