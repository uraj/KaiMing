package edu.psu.ist.plato.kaiming;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

// Static Data Flow Problems refer to those whose kill and gen sets do not
// depend on in sets.
public abstract class DFProblem extends PathInsensitiveProblem<BitSet> {

    private Map<BasicBlock, BitSet> mGenMap, mKillMap;
    
    public DFProblem(Procedure p, CFG cfg, Direction direction) {
        super(p, cfg, direction);
        int size = cfg.getSize();
        mGenMap = new HashMap<BasicBlock, BitSet>(size);
        mKillMap = new HashMap<BasicBlock, BitSet>(size);
        for (BasicBlock bb : mCfg) {
            mGenMap.put(bb, createGenSet(bb));
            mKillMap.put(bb, createKillSet(bb));
        }
    }

    protected abstract BitSet createGenSet(BasicBlock BB);

    protected abstract BitSet createKillSet(BasicBlock BB);

    protected abstract BitSet getInitialEntryState();
    
    @Override
    protected final BitSet getInitialEntryState(BasicBlock bb) {
        return getInitialEntryState();
    }
    
    @Override
    protected final BitSet transfer(BasicBlock bb, BitSet inset) {
        BitSet ret = new BitSet(inset.size());
        ret.or(inset);
        ret.andNot(mKillMap.get(bb));
        ret.or(mGenMap.get(bb));
        return ret;
    }
}
