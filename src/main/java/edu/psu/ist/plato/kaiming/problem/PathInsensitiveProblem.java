package edu.psu.ist.plato.kaiming.problem;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Procedure;
import edu.psu.ist.plato.kaiming.util.Assert;

public abstract class PathInsensitiveProblem<T> extends Problem<Map<BasicBlock, T>> {
    public enum Direction {
        FORWARD, BACKWARD
    };

    protected Procedure mP;
    protected final CFG mCfg;
    private boolean mSolved;
    private Direction mDirection;
    private static final int sMaxIterMultiplier = 10;
    private int mMaxIter;

    public PathInsensitiveProblem(Procedure p, CFG cfg, Direction direction) {
        super(null);
        mSolved = false;
        mP = p;
        mCfg = cfg;
        mDirection = direction;
        mMaxIter = sMaxIterMultiplier * mCfg.getSize(); 
    }
    
    public void setMaxIteration(int max) {
        mMaxIter = max;
    }
    
    public int getMaxIteration() {
        return mMaxIter;
    }

    protected abstract T getInitialEntryState(BasicBlock bb);

    protected abstract T transfer(BasicBlock bb, T in) throws UnsolvableException;

    protected abstract T confluence(Set<T> dataset);

    protected static final BitSet finiteUnion(Set<BitSet> all) {
        Assert.test(all.size() > 0);

        BitSet ret = new BitSet();
        for (BitSet next : all) {
            ret.or(next);
        }
        return ret;
    }

    protected static final BitSet finiteIntersect(Set<BitSet> all) {
        assert all.size() > 0;

        BitSet ret = new BitSet();
        Iterator<BitSet> i = all.iterator();
        ret.or(i.next());
        while (i.hasNext()) {
            ret.and(i.next());
        }
        return ret;
    }

    @Override
    final public void solve() throws UnsolvableException {
        if (mSolved) {
            throw new UnsolvableException("Problem already solved.", 
                    UnsolvableException.Reason.SOLVED);
        }

        int size = mCfg.getSize();
        Map<BasicBlock, T> entryMap = new HashMap<BasicBlock, T>(size);
        Map<BasicBlock, T> exitMap = new HashMap<BasicBlock, T>(size);
        
        boolean dirty;
        for (BasicBlock bb : mCfg) {
            T init = getInitialEntryState(bb);
            entryMap.put(bb, init);
            T exit = transfer(bb, init);
            exitMap.put(bb, exit);
        }

        int roundLeft = mMaxIter;
        do {
            dirty = false;
            for (BasicBlock bb : mCfg) {
                HashSet<T> confluenceSet = new HashSet<T>();
                switch (mDirection) {
                    case FORWARD: {
                        Iterator<BasicBlock> i = bb.iterPredecessor();
                        while (i.hasNext()) {
                            BasicBlock p = i.next();
                            T value = exitMap.get(p);
                            Assert.test(value != null);
                            confluenceSet.add(value);
                        }
                    }
                    case BACKWARD: {
                        Iterator<BasicBlock> i = bb.iterSuccessor();
                        while (i.hasNext()) {
                            BasicBlock s = i.next();
                            T value = exitMap.get(s);
                            Assert.test(value != null);
                            confluenceSet.add(value);
                        }
                    }
                }
                if (confluenceSet.size() > 0) {
                    T entryNew = confluence(confluenceSet);
                    if (!entryNew.equals(entryMap.get(bb))) {
                        entryMap.put(bb, entryNew);
                        dirty = true;
                        T exitNew = transfer(bb, entryNew);
                        exitMap.put(bb, exitNew);
                    }
                }
            }
            --roundLeft;
        } while (dirty && roundLeft > 0);
        
        if (dirty && roundLeft == 0) {
            throw new UnsolvableException(String.format(
                    "Solution failed to converge within %d steps.", mMaxIter),
                    UnsolvableException.Reason.REACH_MAX_ITERATION);
        }
        setSolution(exitMap);
        mSolved = true;
    }
}
