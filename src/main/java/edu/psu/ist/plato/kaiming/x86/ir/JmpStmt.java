package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.Set;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.x86.Flag;
import edu.psu.ist.plato.kaiming.x86.JumpInst;

public class JmpStmt extends Stmt {

    private Expr mTarget;
    
    public JmpStmt(JumpInst inst, Expr target) {
        super(Kind.JMP, inst, new Expr[] {target});
        mTarget = target;
    }
    
    public Expr target() {
        return mTarget;
    }
    
    public boolean isConditional() {
    	return ((JumpInst)mInst).isConditional();
    }
    
    public Set<Flag> dependentFlags() {
        return ((JumpInst)mInst).dependentFlags();
    }
    
    public void resolveTarget(BasicBlock<Stmt> bb) {
        mTarget = mTarget.asTarget(bb);
    }
}
