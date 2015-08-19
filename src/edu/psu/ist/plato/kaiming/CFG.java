package edu.psu.ist.plato.kaiming;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class CFG implements Iterable<BasicBlock> {

    /* Basic blocks of x86 assembly in a CFG MUST be ordered,
     * otherwise the semantics will go wrong.
     * */
    private SortedSet<BasicBlock> mBBs;
    private BasicBlock mEntry;

    /* The only way to create a CFG in your own analysis
     * should be by extending the Procedure abstract class
     * and use its protected method createCFGObject
     * */
    CFG(Collection<BasicBlock> bbs, BasicBlock entry) {
        for (BasicBlock bb : bbs) {
            if (bb == null)
                throw new IllegalArgumentException();
        }
        mBBs = new TreeSet<BasicBlock>(bbs);
        mEntry = entry;
    }

    @Override
    public Iterator<BasicBlock> iterator() {
        return mBBs.iterator();
    }

    public BasicBlock getEntryBlock() {
        return mEntry;
    }

    public int getSize() {
        return mBBs.size();
    }
    
    public void addBasicBlock(BasicBlock bb) {
        if (bb == null)
            throw new IllegalArgumentException();
        mBBs.add(bb);
    }
    
    public void addBasicBlockAll(Collection<? extends BasicBlock> bbs) {
        for (BasicBlock bb : bbs) {
            if (bb == null)
                throw new IllegalArgumentException();
        }
        mBBs.addAll(bbs);
    }
    
    public void setEntryBlock(BasicBlock entry) {
        mEntry = entry;
    }
    
    public List<? extends Entry> getEntries() {
        List<Entry> ret = new ArrayList<Entry>();
        for (BasicBlock bb : this) {
            ret.addAll(bb.mEntries);
        }
        return ret;
    }
}
