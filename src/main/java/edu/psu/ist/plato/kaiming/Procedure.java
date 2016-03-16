package edu.psu.ist.plato.kaiming;

import java.util.Collection;
import java.util.List;

public abstract class Procedure {
    
    /**
     * A procedure is required to have a CFG, even if it is only a trivial one.
     */
    public abstract CFG getCFG();

    public abstract void setEntries(List<? extends Entry> entries);
    
    public List<? extends Entry> getEntries() {
        return getCFG().getEntries();
    }

    protected CFG createCFGObject(Collection<BasicBlock> bbs,
            BasicBlock entry) {
        return new CFG(bbs, entry);
    }
    
    public abstract String getName();

}
