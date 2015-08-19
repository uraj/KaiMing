package edu.psu.ist.plato.kaiming;

import java.util.Collection;
import java.util.List;

public abstract class Procedure {
    /**
     * A procedure is required to have a CFG, even if it is only a trivial one.
     */
    protected CFG mCfg;
    protected Label mLabel;

    public Procedure(Label l, List<? extends Entry> entries) {
        mLabel = l;
        mCfg = buildCFGInternal(entries);
    }

    public void setEntries(List<? extends Entry> entries) {
        mCfg = buildCFGInternal(entries);
    }

    public List<? extends Entry> getEntries() {
        return mCfg.getEntries();
    }

    public abstract CFG buildCFGInternal(List<? extends Entry> entries);

    public CFG getCFG() {
        return mCfg;
    }

    public abstract String getName();

    public abstract Label deriveSubLabel(BasicBlock bb);

    protected CFG createCFGObject(Collection<BasicBlock> bbs,
            BasicBlock entry) {
        return new CFG(bbs, entry);
    }
}
