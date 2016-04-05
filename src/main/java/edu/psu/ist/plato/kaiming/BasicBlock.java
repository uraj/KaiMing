package edu.psu.ist.plato.kaiming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import edu.psu.ist.plato.kaiming.util.ReverseIterator;

public class BasicBlock<T extends Entry> implements Iterable<T>, Comparable<BasicBlock<T>> {

    protected List<BasicBlock<T>> mPred;
    protected List<BasicBlock<T>> mSucc;

    protected List<T> mEntries;

    protected Label mLabel;

    protected Procedure<T> mUnit;
    
    private class ReverseEntryIterator implements ReverseIterator<T> {

        ListIterator<T> mIter;
        
        public ReverseEntryIterator(List<T> entries) {
            mIter = entries.listIterator(entries.size());
        }
        
        @Override
        public boolean hasPrevious() {
            return mIter.hasPrevious();
        }

        @Override
        public T previous() {
            return mIter.previous();
        }

        @Override
        public void remove() {
            mIter.remove();
        }
    }

    public BasicBlock(Procedure<T> unit, List<T> entries, Label label) {
        mEntries = new LinkedList<T>(entries);
        mLabel = label;
        mPred = new LinkedList<BasicBlock<T>>();
        mSucc = new LinkedList<BasicBlock<T>>();
        mUnit = unit;
    }
    
    public long index() {
        return firstEntry().index();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public boolean equals(Object obj) {
        if (obj instanceof BasicBlock) {
            return compareTo((BasicBlock<T>)obj) == 0;
        } else {
            return super.hashCode() == obj.hashCode();
        }
    }
    
    @Override
    public int hashCode() {
        return (int)index();
    }

    @Override
    public Iterator<T> iterator() {
        return mEntries.iterator();
    }
    
    public int size() {
        return mEntries.size();
    }
    
    public List<T> entries() {
        return new ArrayList<T>(mEntries);
    }
    
    public ReverseIterator<T> reverseIterator() {
        return new ReverseEntryIterator(mEntries);
    }

    public T firstEntry() {
        return mEntries.get(0);
    }

    public T lastEntry() {
        return mEntries.get(mEntries.size() - 1);
    }

    public boolean hasPredecessor() {
        return mPred.size() != 0;
    }

    public boolean hasSuccessor() {
        return mSucc.size() != 0;
    }

    public boolean addPredecessor(BasicBlock<T> e) {
        return mPred.add(e);
    }

    public boolean addSuccessor(BasicBlock<T> e) {
        return mSucc.add(e);
    }
    
    public boolean addPredecessorAll(Collection<? extends BasicBlock<T>> e) {
        return mPred.addAll(e);
    }

    public boolean addSuccessorAll(Collection<? extends BasicBlock<T>> e) {
        return mSucc.addAll(e);
    }

    public BasicBlock<T> predecessor(int pos) {
        return mPred.get(pos);
    }
    
    public BasicBlock<T> successor(int pos) {
        return mSucc.get(pos);
    }
    
    public List<BasicBlock<T>> allPredecessor() {
        return new ArrayList<BasicBlock<T>>(mPred);
    }
    
    public List<BasicBlock<T>> allSuccessor() {
        return new ArrayList<BasicBlock<T>>(mSucc);
    }
    
    public BasicBlock<T> removePredecessor(int pos) {
        return mPred.remove(pos);
    }

    public BasicBlock<T> removeSuccessor(int pos) {
        return mSucc.remove(pos);
    }

    public boolean removePredecessor(BasicBlock<T> bb) {
        return mPred.remove(bb);
    }

    public boolean removeSuccessor(BasicBlock<T> bb) {
        return mSucc.remove(bb);
    }
    
    public void removePredecessorAll() {
        mPred.clear();
    }

    public void removeSuccessorAll() {
        mSucc.clear();
    }
    
    public Iterator<BasicBlock<T>> iterPredecessor() {
        return mPred.iterator();
    }

    public Iterator<BasicBlock<T>> iterSuccessor() {
        return mSucc.iterator();
    }
    
    public int numOfPredecessor() {
        return mPred.size();
    }
    
    public int numOfSuccessor() {
        return mSucc.size();
    }
    
    public Label label() {
        return mLabel;
    }

    public void setLable(Label label) {
        mLabel = label;
    }

    
    public static <I extends Entry> ArrayList<BasicBlock<I>> split(Procedure<I> unit,
            final List<I> entries, Integer[] pivots) {
        
        if (pivots == null)
            return null;
        
        if (pivots.length == 0) {
            ArrayList<BasicBlock<I>> ret = new ArrayList<BasicBlock<I>>();
            ret.add(new BasicBlock<I>(unit, entries, null));
            return ret;
        }

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
        
        BasicBlock<I> bb;
        ArrayList<BasicBlock<I>> bbs = new ArrayList<BasicBlock<I>>(length + 1);
        for (i = 0; i < length; ++i, ++j) {
            bb = new BasicBlock<I>(unit, entries.subList(prevPivot, pivots[j]),
                    null);
            bbs.add(bb);
            prevPivot = pivots[j];
        }
        bb = new BasicBlock<I>(unit, entries.subList(prevPivot, entries.size()),
                null);
        bbs.add(bb);
        return bbs;
    }

    public ArrayList<BasicBlock<T>> split(Integer[] pivots) {
        ArrayList<BasicBlock<T>> bbs = BasicBlock.split(mUnit, mEntries, pivots);

        bbs.get(0).setLable(this.label());
        for (int i = 1; i < bbs.size(); ++i) {
            bbs.get(i).setLable(mUnit.deriveSubLabel(bbs.get(i)));
        }

        for (int i = 0; i < bbs.size() - 1; ++i) {
            Iterator<BasicBlock<T>> iterSucc = bbs.get(i + 1).iterPredecessor();
            while (iterSucc.hasNext()) {
                bbs.get(i).addSuccessor(iterSucc.next());
            }
        }

        for (int i = 1; i < bbs.size(); ++i) {
            Iterator<BasicBlock<T>> iterPred = bbs.get(i - 1).iterSuccessor();
            while (iterPred.hasNext()) {
                bbs.get(i).addPredecessor(iterPred.next());
            }
        }

        BasicBlock<T> first = bbs.get(0), last = bbs.get(bbs.size() - 1);
        Iterator<BasicBlock<T>> iterPred = iterPredecessor();
        while (iterPred.hasNext()) {
            BasicBlock<T> pred = iterPred.next();
            first.addPredecessor(pred);
            pred.removeSuccessor(this);
            pred.addSuccessor(first);
        }
        Iterator<BasicBlock<T>> iterSucc = iterSuccessor();
        while (iterSucc.hasNext()) {
            BasicBlock<T> succ = iterSucc.next();
            last.addPredecessor(succ);
            succ.removePredecessor(this);
            succ.addPredecessor(last);
        }

        return bbs;
    }
    
    public ArrayList<BasicBlock<T>> split(Long index) {
        int pivot = Entry.searchIndex(mEntries.toArray(new Entry[0]), index);
        return split(new Integer[] {pivot+1});
    }

    
    @Override
    public int compareTo(BasicBlock<T> bb) {
        return Long.signum(index() - bb.index());
    }
    
    public static <I extends Entry> BasicBlock<I> searchContainingBlock(final BasicBlock<I>[] bbs,
            long addr) {
        return searchContainingBlock(Arrays.asList(bbs), addr);
    }
    
    public static <I extends Entry> BasicBlock<I> searchContainingBlock(final Iterable<BasicBlock<I>> bbs,
            long addr) {
        for (BasicBlock<I> bb : bbs) {
            if (bb.lastEntry().compareTo(addr) >= 0
                    && bb.firstEntry().compareTo(addr) <= 0) {
                return bb;
            }
        }
        return null;
    }
}
