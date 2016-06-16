package edu.psu.ist.plato.kaiming.ir;

import edu.psu.ist.plato.kaiming.x86.Instruction;

public class RetStmt extends Stmt {
    private static final Expr[] sEmpty = new Expr[] {};
    
    public RetStmt(Instruction inst) {
        super(Kind.RET, inst, sEmpty);
    }
}
