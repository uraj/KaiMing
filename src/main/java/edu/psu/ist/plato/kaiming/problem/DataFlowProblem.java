package edu.psu.ist.plato.kaiming.problem;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Procedure;

// Static Data Flow Problems refer to those whose kill and gen sets do not
// depend on in sets.
public abstract class DataFlowProblem<T extends Entry> extends PathInsensitiveProblem<T, BitSet> {

    private Map<BasicBlock<T>, BitSet> mGenMap, mKillMap;
    
    public DataFlowProblem(Procedure<T> p, CFG<T> cfg, Direction direction) {
        super(p, cfg, direction);
        int size = cfg.size();
        mGenMap = new HashMap<BasicBlock<T>, BitSet>(size);
        mKillMap = new HashMap<BasicBlock<T>, BitSet>(size);
        for (BasicBlock<T> bb : mCfg) {
            mGenMap.put(bb, createGenSet(bb));
            mKillMap.put(bb, createKillSet(bb));
        }
    }

    protected abstract BitSet createGenSet(BasicBlock<T> BB);

    protected abstract BitSet createKillSet(BasicBlock<T> BB);

    protected abstract BitSet getInitialEntryState();
    
    @Override
    protected final BitSet getInitialEntryState(BasicBlock<T> bb) {
        return getInitialEntryState();
    }
    
    @Override
    protected final BitSet transfer(BasicBlock<T> bb, BitSet inset) {
        BitSet ret = new BitSet(inset.size());
        ret.or(inset);
        ret.andNot(mKillMap.get(bb));
        ret.or(mGenMap.get(bb));
        return ret;
    }
}
