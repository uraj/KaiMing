package edu.psu.ist.plato.kaiming.problem;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Procedure;
import edu.psu.ist.plato.kaiming.util.Assert;

public abstract class PathInsensitiveProblem<E extends Entry, T> extends Problem<Map<BasicBlock<E>, T>> {
    public enum Direction {
        FORWARD, BACKWARD
    };

    protected Procedure<E> mP;
    protected final CFG<E> mCfg;
    private boolean mSolved;
    private Direction mDirection;
    private static final int sMaxIterMultiplier = 10;
    private int mMaxIter;

    public PathInsensitiveProblem(Procedure<E> p, CFG<E> cfg, Direction direction) {
        super(null);
        mSolved = false;
        mP = p;
        mCfg = cfg;
        mDirection = direction;
        mMaxIter = sMaxIterMultiplier * mCfg.size(); 
    }
    
    public void setMaxIteration(int max) {
        mMaxIter = max;
    }
    
    public int getMaxIteration() {
        return mMaxIter;
    }

    protected abstract T getInitialEntryState(BasicBlock<E> bb);

    protected abstract T transfer(BasicBlock<E> bb, T in) throws UnsolvableException;

    protected abstract T confluence(Set<T> dataset);

    protected static final BitSet finiteUnion(Set<BitSet> all) {
        BitSet ret = new BitSet();
        for (BitSet next : all) {
            ret.or(next);
        }
        return ret;
    }

    protected static final BitSet finiteIntersect(Set<BitSet> all) {
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

        int size = mCfg.size();
        Map<BasicBlock<E>, T> entryMap = new HashMap<BasicBlock<E>, T>(size);
        Map<BasicBlock<E>, T> exitMap = new HashMap<BasicBlock<E>, T>(size);
        
        boolean dirty;
        for (BasicBlock<E> bb : mCfg) {
            T init = getInitialEntryState(bb);
            entryMap.put(bb, init);
            T exit = transfer(bb, init);
            exitMap.put(bb, exit);
        }

        int roundLeft = mMaxIter;
        do {
            dirty = false;
            for (BasicBlock<E> bb : mCfg) {
                HashSet<T> confluenceSet = new HashSet<T>();
                switch (mDirection) {
                    case FORWARD: {
                        for (BasicBlock<E> p : bb.allPredecessor()) {
                            T value = exitMap.get(p);
                            Assert.test(value != null);
                            confluenceSet.add(value);
                        }
                        break;
                    }
                    case BACKWARD: {
                        for (BasicBlock<E> s : bb.allSuccessor()) {
                            T value = exitMap.get(s);
                            Assert.test(value != null);
                            confluenceSet.add(value);
                        }
                        break;
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
