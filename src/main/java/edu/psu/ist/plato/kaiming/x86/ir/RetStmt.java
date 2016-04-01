package edu.psu.ist.plato.kaiming.x86.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

public class RetStmt extends Stmt {
    protected RetStmt(Instruction inst) {
        super(Kind.RET, inst);
    }
}
