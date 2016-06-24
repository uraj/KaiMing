package edu.psu.ist.plato.kaiming.ir;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.Entry;

public class JmpStmt extends Stmt {

    private Target mResolvedTarget;
    private Set<Flg> mDependentFlags;
    
    public JmpStmt(Entry inst, Expr target, Collection<Flg> flags) {
        super(Kind.JMP, inst, new Expr[] {target});
        mResolvedTarget = null;
        mDependentFlags = new HashSet<Flg>(flags);
        for (Flg f : mDependentFlags) {
            updateDefFor(f, new HashSet<DefStmt>());
        }
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
    	return mDependentFlags.size() > 0;
    }
    
    public Set<Flg> dependentFlags() {
        return mDependentFlags;
    }
    
    public void resolveTarget(BasicBlock<Stmt> bb) {
        mResolvedTarget = usedExpr(0).asTarget(bb);
    }
}
