package edu.psu.ist.plato.kaiming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class CFG<T extends Entry> implements Iterable<BasicBlock<T>> {

    /* Basic blocks of x86 assembly in a CFG MUST be ordered,
     * otherwise the semantics will go wrong.
     * */
    private SortedSet<BasicBlock<T>> mBBs;
    private BasicBlock<T> mEntry;

    /* The only way to create a CFG in your own analysis
     * should be by extending the Procedure abstract class
     * and use its protected method createCFGObject
     * */
    CFG(Collection<BasicBlock<T>> bbs, BasicBlock<T> entry) {
        for (BasicBlock<T> bb : bbs) {
            if (bb == null)
                throw new IllegalArgumentException();
        }
        mBBs = new TreeSet<BasicBlock<T>>(bbs);
        mEntry = entry;
    }
    
    @Override 
    public CFG<T> clone() {
        return new CFG<T>(mBBs, mEntry);
    }

    @Override
    public Iterator<BasicBlock<T>> iterator() {
        return mBBs.iterator();
    }

    public BasicBlock<T> getEntryBlock() {
        return mEntry;
    }

    public int getSize() {
        return mBBs.size();
    }
    
    public void addBasicBlock(BasicBlock<T> bb) {
        if (bb == null)
            throw new IllegalArgumentException();
        mBBs.add(bb);
    }
    
    public void addBasicBlockAll(Collection<? extends BasicBlock<T>> bbs) {
        for (BasicBlock<T> bb : bbs) {
            if (bb == null)
                throw new IllegalArgumentException();
        }
        mBBs.addAll(bbs);
    }
    
    public void removeBasicBlock(BasicBlock<T> bb) {
        mBBs.remove(bb);
    }
    
    public void removeBasicBlockAll(Collection<? extends BasicBlock<T>> bbs) {
        for (BasicBlock<T> bb : bbs) {
            if (bb == null)
                throw new IllegalArgumentException();
        }
        mBBs.removeAll(bbs);
    }
    
    public void setEntryBlock(BasicBlock<T> entry) {
        mEntry = entry;
    }
    
    public List<T> getEntries() {
        List<T> ret = new ArrayList<T>();
        for (BasicBlock<T> bb : this) {
            ret.addAll(bb.mEntries);
        }
        return ret;
    }
    
    public int getNumEntries() {
        int ret = 0;
        for (BasicBlock<T> bb : this) {
            ret += bb.size();
        }
        return ret;
    }
    
    public BasicBlock<T> searchContainingBlock(long index) {
        return BasicBlock.searchContainingBlock(mBBs, index);
    }
}
