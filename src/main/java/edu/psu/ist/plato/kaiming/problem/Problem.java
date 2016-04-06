package edu.psu.ist.plato.kaiming.problem;

public abstract class Problem<T> {

    private T mSolution;
    
    protected Problem(T emptySolution) {
        mSolution = emptySolution;
    }
    
    public abstract void solve() throws UnsolvableException;

    public final T solution() {
        return mSolution;
    };
    
    protected final void setSolution(T solution) {
        mSolution = solution;
    }
    
}
