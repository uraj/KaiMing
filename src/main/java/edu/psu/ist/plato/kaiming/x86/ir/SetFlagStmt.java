package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.Set;

import edu.psu.ist.plato.kaiming.x86.Instruction;
import edu.psu.ist.plato.kaiming.x86.Flag;

public class SetFlagStmt extends Stmt {

    
    protected SetFlagStmt(Instruction inst) {
        super(Kind.SETF, inst);
    }
    
    public Set<Flag> affectedFlags() {
        return mInst.getModifiedFlags();
    }
}
