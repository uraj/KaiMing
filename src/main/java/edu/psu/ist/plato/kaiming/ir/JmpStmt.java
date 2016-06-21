package edu.psu.ist.plato.kaiming.ir;

import java.util.Set;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.x86.Flag;
import edu.psu.ist.plato.kaiming.x86.JumpInst;

public class JmpStmt extends Stmt {

    private Target mResolvedTarget;
    
    public JmpStmt(JumpInst inst, Expr target) {
        super(Kind.JMP, inst, new Expr[] {target});
        mResolvedTarget = null;
    }
    
    public Expr target() {
        return usedExpr(0);
    }
    
    public Target resolvedTarget() {
        return mResolvedTarget;
    }
    
    public boolean hasResolvedTarget() {
        return mResolvedTarget != null;
    }
    
    public boolean isConditional() {
    	return ((JumpInst)mHost).isConditional();
    }
    
    public Set<Flag> dependentFlags() {
        return ((JumpInst)mHost).dependentFlags();
    }
    
    public void resolveTarget(BasicBlock<Stmt> bb) {
        mResolvedTarget = usedExpr(0).asTarget(bb);
    }
}
