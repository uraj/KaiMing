package edu.psu.ist.plato.kaiming;

import java.util.Collection;

public abstract class FlowInsensitiveProblem<T> {
    protected Procedure mP;
    private Collection<? extends Entry> mEntries;
    private T mSolution;
    private boolean mSolved;
    
    public FlowInsensitiveProblem(Procedure p, Collection<? extends Entry> entryFlow) {
        mSolved = false;
        mSolution = null;
        mP = p;
        mEntries = entryFlow;
    }

    protected abstract T getInitialEntryState();

    protected abstract T process(Entry e, T in) throws UnsolvableException;

    public T getSolution() {
        return mSolution;
    }

    public final void solve() throws UnsolvableException {
        if (mSolved) {
            throw new UnsolvableException("Problem already solved.", 
                    UnsolvableException.Reason.SOLVED);
        }
        mSolution = getInitialEntryState();
        for (Entry e : mEntries) {
            try {
                mSolution = process(e, mSolution);
            } catch (UnsolvableException except) {
                if (except.reason == UnsolvableException.Reason.SOLVED) {
                    break;
                } else {
                    mSolution = null;
                    throw except;
                }
            }
        }
        mSolved = true;
    }
}
