package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.util.Tuple;
import edu.psu.ist.plato.kaiming.x86.Instruction;

public class AssignStmt extends DefStmt {

    private Tuple<Integer, Integer> mUpdateRange;

    public AssignStmt(Instruction inst, Lval lval, Expr expr) {
        super(Kind.ASSIGN, inst, lval, new Expr[] { expr });
        mUpdateRange = new Tuple<Integer, Integer>(0, inst.machine().wordSizeInBits());
    }
    
    // [rangeLow, rangeHigh)
    public AssignStmt(Instruction inst, Lval lval, Expr expr, int rangeLow, int rangeHigh) {
        super(Kind.ASSIGN, inst, lval, new Expr[] { expr });
        mUpdateRange = new Tuple<Integer, Integer>(rangeLow, rangeHigh);
    }
    
    public Expr usedRval() {
        return usedExpr(0);
    }

    public Tuple<Integer, Integer> rangeOfAssignment() {
        return mUpdateRange;
    }
}
