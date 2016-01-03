package edu.psu.ist.plato.kaiming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import edu.psu.ist.plato.kaiming.util.ReverseIterator;

public class BasicBlock implements Iterable<Entry>, Comparable<BasicBlock> {

    protected List<BasicBlock> mPred;
    protected List<BasicBlock> mSucc;

    protected List<Entry> mEntries;

    protected Label mLabel;

    protected Procedure mUnit;
    
    private class ReverseEntryIterator implements ReverseIterator<Entry> {

        ListIterator<Entry> mIter;
        
        public ReverseEntryIterator(List<Entry> entries) {
            mIter = entries.listIterator(entries.size());
        }
        
        @Override
        public boolean hasPrevious() {
            return mIter.hasPrevious();
        }

        @Override
        public Entry previous() {
            return mIter.previous();
        }

        @Override
        public void remove() {
            mIter.remove();
        }
    }

    public BasicBlock(Procedure unit, List<? extends Entry> entries, Label label) {
        mEntries = new LinkedList<Entry>(entries);
        mLabel = label;
        mPred = new LinkedList<BasicBlock>();
        mSucc = new LinkedList<BasicBlock>();
        mUnit = unit;
    }
    
    public long getIndex() {
        return getFirstEntry().getIndex();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicBlock) {
            return compareTo((BasicBlock)obj) == 0;
        } else {
            return super.hashCode() == obj.hashCode();
        }
    }
    
    @Override
    public int hashCode() {
        return (int)getIndex();
    }

    @Override
    public Iterator<Entry> iterator() {
        return mEntries.iterator();
    }
    
    public int size() {
        return mEntries.size();
    }
    
    public Entry[] getEntries() {
        return mEntries.toArray(new Entry[0]);
    }
    
    public ReverseIterator<Entry> reverseIterator() {
        return new ReverseEntryIterator(mEntries);
    }

    public Entry getFirstEntry() {
        return mEntries.get(0);
    }

    public Entry getLastEntry() {
        return mEntries.get(mEntries.size() - 1);
    }

    public boolean hasPredecessor() {
        return mPred.size() != 0;
    }

    public boolean hasSuccessor() {
        return mSucc.size() != 0;
    }

    public boolean addPredecessor(BasicBlock e) {
        return mPred.add(e);
    }

    public boolean addSuccessor(BasicBlock e) {
        return mSucc.add(e);
    }
    
    public boolean addPredecessorAll(Collection<? extends BasicBlock> e) {
        return mPred.addAll(e);
    }

    public boolean addSuccessorAll(Collection<? extends BasicBlock> e) {
        return mSucc.addAll(e);
    }

    public BasicBlock getPredecessor(int pos) {
        return mPred.get(pos);
    }
    
    public BasicBlock getSuccessor(int pos) {
        return mSucc.get(pos);
    }
    
    public List<BasicBlock> getPredecessorAll() {
        return new ArrayList<BasicBlock>(mPred);
    }
    
    public List<BasicBlock> getSuccessorAll() {
        return new ArrayList<BasicBlock>(mSucc);
    }
    
    public BasicBlock removePredecessor(int pos) {
        return mPred.remove(pos);
    }

    public BasicBlock removeSuccessor(int pos) {
        return mSucc.remove(pos);
    }

    public boolean removePredecessor(BasicBlock bb) {
        return mPred.remove(bb);
    }

    public boolean removeSuccessor(BasicBlock bb) {
        return mSucc.remove(bb);
    }
    
    public void removePredecessorAll() {
        mPred.clear();
    }

    public void removeSuccessorAll() {
        mSucc.clear();
    }
    
    public Iterator<BasicBlock> iterPredecessor() {
        return mPred.iterator();
    }

    public Iterator<BasicBlock> iterSuccessor() {
        return mSucc.iterator();
    }
    
    public int getNumPredecessor() {
        return mPred.size();
    }
    
    public int getNumSuccessor() {
        return mSucc.size();
    }
    
    public Label getLabel() {
        return mLabel;
    }

    public void setLable(Label label) {
        mLabel = label;
    }

    public static BasicBlock[] split(Procedure unit,
            final List<? extends Entry> entries, Integer[] pivots) {
        
        if (pivots == null)
            return null;
        
        if (pivots.length == 0)
            return new BasicBlock[] { new BasicBlock(unit, entries, null) };

        Arrays.sort(pivots);
        if (pivots[pivots.length - 1] > entries.size())
            return null;
        
        int length = pivots.length;
        if (pivots[pivots.length - 1] == entries.size())
            --length;
        if (pivots[0] == 0)
            --length;
        
        int prevPivot = 0;
        int i, j;
        if (pivots[0] == 0) {
            j = 1;
        } else {
            j = 0;
        }
        
        BasicBlock bb;
        BasicBlock[] bbs = new BasicBlock[length + 1];
        for (i = 0; i < length; ++i, ++j) {
            bb = new BasicBlock(unit, entries.subList(prevPivot, pivots[j]),
                    null);
            bbs[i] = bb;
            prevPivot = pivots[j];
        }
        bb = new BasicBlock(unit, entries.subList(prevPivot, entries.size()),
                null);
        bbs[i] = bb;
        return bbs;
    }

    public BasicBlock[] split(Integer[] pivots) {
        BasicBlock[] bbs = BasicBlock.split(mUnit, mEntries, pivots);

        bbs[0].setLable(this.getLabel());
        for (int i = 1; i < bbs.length; ++i) {
            bbs[i].setLable(mUnit.deriveSubLabel(bbs[i]));
        }

        for (int i = 0; i < bbs.length - 1; ++i) {
            Iterator<BasicBlock> iterSucc = bbs[i + 1].iterPredecessor();
            while (iterSucc.hasNext()) {
                bbs[i].addSuccessor(iterSucc.next());
            }
        }

        for (int i = 1; i < bbs.length; ++i) {
            Iterator<BasicBlock> iterPred = bbs[i - 1].iterSuccessor();
            while (iterPred.hasNext()) {
                bbs[i].addPredecessor(iterPred.next());
            }
        }

        BasicBlock first = bbs[0], last = bbs[bbs.length - 1];
        Iterator<BasicBlock> iterPred = iterPredecessor();
        while (iterPred.hasNext()) {
            BasicBlock pred = iterPred.next();
            first.addPredecessor(pred);
            pred.removeSuccessor(this);
            pred.addSuccessor(first);
        }
        Iterator<BasicBlock> iterSucc = iterSuccessor();
        while (iterSucc.hasNext()) {
            BasicBlock succ = iterSucc.next();
            last.addPredecessor(succ);
            succ.removePredecessor(this);
            succ.addPredecessor(last);
        }

        return bbs;
    }
    
    public BasicBlock[] split(Long index) {
        int pivot = Entry.searchIndex(mEntries.toArray(new Entry[0]), index);
        return split(new Integer[] {pivot+1});
    }

    
    @Override
    public int compareTo(BasicBlock bb) {
        return Long.signum(getIndex() - bb.getIndex());
    }
    
    public static BasicBlock searchContainingBlock(final BasicBlock[] bbs,
            long addr) {
        return searchContainingBlock(Arrays.asList(bbs), addr);
    }
    
    public static BasicBlock searchContainingBlock(final Iterable<BasicBlock> bbs,
            long addr) {
        for (BasicBlock bb : bbs) {
            if (bb.getLastEntry().compareTo(addr) >= 0
                    && bb.getFirstEntry().compareTo(addr) <= 0) {
                return bb;
            }
        }
        return null;
    }
}
