package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.Set;

import edu.psu.ist.plato.kaiming.x86.Instruction;
import edu.psu.ist.plato.kaiming.x86.Flag;

public class SetFlagStmt extends Stmt {

    
    public SetFlagStmt(Instruction inst) {
        super(Kind.SETF, inst, new Expr[] {});
    }
    
    public Set<Flag> affectedFlags() {
        return mInst.modifiedFlags();
    }
}