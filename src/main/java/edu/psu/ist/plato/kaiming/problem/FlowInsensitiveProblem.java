package edu.psu.ist.plato.kaiming.problem;

import java.util.Collection;

import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Procedure;

public abstract class FlowInsensitiveProblem<E extends Entry, T> extends Problem<T> {
    protected Procedure<E> mP;
    private Collection<E> mEntries;
    private boolean mSolved;
    
    public FlowInsensitiveProblem(Procedure<E> p, Collection<E> entryFlow) {
        super(null);
        mSolved = false;
        mP = p;
        mEntries = entryFlow;
    }

    protected abstract T getInitialEntryState();

    protected abstract T process(E e, T in) throws UnsolvableException;

    @Override
    public final void solve() throws UnsolvableException {
        if (mSolved) {
            throw new UnsolvableException("Problem already solved.", 
                    UnsolvableException.Reason.SOLVED);
        }
        T solution;
        solution = getInitialEntryState();
        for (E e : mEntries) {
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
