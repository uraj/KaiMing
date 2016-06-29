package edu.psu.ist.plato.kaiming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import edu.psu.ist.plato.kaiming.util.Assert;

public class BasicBlock<T extends Entry> implements Iterable<T>, Comparable<BasicBlock<T>> {

    protected List<BasicBlock<T>> mPred;
    protected List<BasicBlock<T>> mSucc;
    protected LinkedList<T> mEntries;
    protected Label mLabel;
    protected Procedure<T> mUnit;

    public BasicBlock(Procedure<T> unit, List<T> entries, Label label) {
        if (entries.size() == 0)
            throw new IllegalArgumentException("A basic block has at least one entry");
        mEntries = new LinkedList<T>(entries);
        mLabel = label;
        mPred = new LinkedList<BasicBlock<T>>();
        mSucc = new LinkedList<BasicBlock<T>>();
        mUnit = unit;
    }
    
    
    // TODO: current implementation is very inefficient for its O(n) complexity
    public boolean insertAfter(T where, T entry) {
        ListIterator<T> i = mEntries.listIterator();
        while (i.hasNext()) {
            if (i.next().equals(where)) {
                i.add(entry);
                return true;
            }
        }
        return false;
    }
    
    public boolean insertBefore(T where, T entry) {
        ListIterator<T> i = mEntries.listIterator();
        while (i.hasNext()) {
            if (i.next().equals(where)) {
                i.previous();
                i.add(entry);
                return true;
            }
        }
        return false;
    }
    
    public boolean remove(T entry) {
        return mEntries.remove(entry);
    }
    
    public long index() {
        return firstEntry().index();
    }
    
    @Override
    public int compareTo(BasicBlock<T> bb) {
        return Long.signum(index() - bb.index());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BasicBlock) {
            return ((BasicBlock<?>)obj).index() == index();
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return (int)index();
    }

    @Override
    public ListIterator<T> iterator() {
        return mEntries.listIterator();
    }
    
    public int size() {
        return mEntries.size();
    }
    
    public List<T> entries() {
        return new ArrayList<T>(mEntries);
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
        Assert.verify(e != null);
        return mPred.add(e);
    }

    public boolean addSuccessor(BasicBlock<T> e) {
        Assert.verify(e != null);
        return mSucc.add(e);
    }
    
    public boolean addPredecessors(Collection<? extends BasicBlock<T>> e) {
        return mPred.addAll(e);
    }

    public boolean addSuccessors(Collection<? extends BasicBlock<T>> e) {
        return mSucc.addAll(e);
    }

    public BasicBlock<T> predecessor(int pos) {
        return mPred.get(pos);
    }
    
    public BasicBlock<T> successor(int pos) {
        return mSucc.get(pos);
    }
    
    public List<BasicBlock<T>> predecessors() {
        return Collections.unmodifiableList(mPred);
    }
    
    public List<BasicBlock<T>> successors() {
        return Collections.unmodifiableList(mSucc);
    }
    
    public BasicBlock<T> removePredecessor(int pos) {
        return mPred.remove(pos);
    }

    public BasicBlock<T> removeSuccessor(int pos) {
        return mSucc.remove(pos);
    }

    public boolean removePredecessors(BasicBlock<T> bb) {
        return mPred.remove(bb);
    }

    public boolean removeSuccessors(BasicBlock<T> bb) {
        return mSucc.remove(bb);
    }
    
    public void clearPredecessors() {
        mPred.clear();
    }

    public void clearSuccessors() {
        mSucc.clear();
    }

    public int numOfPredecessors() {
        return mPred.size();
    }
    
    public int numOfSuccessors() {
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
            final int j = i;
            bbs.get(j + 1).predecessors().forEach(
                    pred -> bbs.get(j).addSuccessor(pred));
        }

        for (int i = 1; i < bbs.size(); ++i) {
            final int j = i;
            bbs.get(j - 1).successors().forEach(
                    succ -> bbs.get(j).addPredecessor(succ));
        }

        BasicBlock<T> first = bbs.get(0), last = bbs.get(bbs.size() - 1);
        for (BasicBlock<T> pred : predecessors()) {
            first.addPredecessor(pred);
            pred.removeSuccessors(this);
            pred.addSuccessor(first);
        }
        for (BasicBlock<T> succ : successors()) {
            last.addPredecessor(succ);
            succ.removePredecessors(this);
            succ.addPredecessor(last);
        }

        return bbs;
    }
    
    public ArrayList<BasicBlock<T>> split(Long index) {
        int pivot = Entry.searchIndex(mEntries.toArray(new Entry[0]), index);
        return split(new Integer[] {pivot+1});
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
