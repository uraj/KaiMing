package edu.psu.ist.plato.kaiming.x86.ir;

import java.util.Set;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.x86.Flag;
import edu.psu.ist.plato.kaiming.x86.JumpInst;

public class JmpStmt extends Stmt {

    private Expr mTarget;
    private Target mResolvedTarget;
    
    public JmpStmt(JumpInst inst, Expr target) {
        super(Kind.JMP, inst, new Expr[] {target});
        mTarget = target;
        mResolvedTarget = null;
    }
    
    public Expr target() {
        return mTarget;
    }
    
    public Target resolvedTarget() {
        return mResolvedTarget;
    }
    
    public boolean hasResolvedTarget() {
        return mResolvedTarget != null;
    }
    
    public boolean isConditional() {
    	return ((JumpInst)mInst).isConditional();
    }
    
    public Set<Flag> dependentFlags() {
        return ((JumpInst)mInst).dependentFlags();
    }
    
    public void resolveTarget(BasicBlock<Stmt> bb) {
        mResolvedTarget = mTarget.asTarget(bb);
    }
}
