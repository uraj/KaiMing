package edu.psu.ist.plato.kaiming;

import java.util.Collection;
import java.util.List;

public abstract class Procedure<T extends Entry> {
    
    /**
     * A procedure is required to have a CFG, even if it is only a trivial one.
     */
    public abstract CFG<T> cfg();
    
    public List<T> entries() {
        return cfg().entries();
    }

    protected CFG<T> createCFGObject(Collection<BasicBlock<T>> bbs,
            BasicBlock<T> entry) {
        return new CFG<T>(bbs, entry);
    }
    
    public abstract String name();
    
    public abstract Label deriveSubLabel(BasicBlock<T> bb);

}
