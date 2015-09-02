package edu.psu.ist.plato.kaiming.problem;

import java.util.Collection;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Procedure;

public abstract class FlowInsensitiveProblem<T> extends Problem<T> {
    protected Procedure mP;
    private Collection<? extends Entry> mEntries;
    private boolean mSolved;
    
    public FlowInsensitiveProblem(Procedure p, Collection<? extends Entry> entryFlow) {
        super(null);
        mSolved = false;
        mP = p;
        mEntries = entryFlow;
    }

    protected abstract T getInitialEntryState();

    protected abstract T process(Entry e, T in) throws UnsolvableException;

    public final void solve() throws UnsolvableException {
        if (mSolved) {
            throw new UnsolvableException("Problem already solved.", 
                    UnsolvableException.Reason.SOLVED);
        }
        T solution;
        solution = getInitialEntryState();
        for (Entry e : mEntries) {
            try {
                solution = process(e, solution);
            } catch (UnsolvableException except) {
                if (except.reason == UnsolvableException.Reason.SOLVED) {
                    setSolution(solution);
                    break;
                } else {
                    setSolution(null);
                    throw except;
                }
            }
        }
        setSolution(solution);
        mSolved = true;
    }
}
