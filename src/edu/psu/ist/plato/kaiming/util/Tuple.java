package edu.psu.ist.plato.kaiming.util;

public class Tuple<F, S> {
    public F first;
    public S second;
    
    public Tuple() {
  	    first = null;
  	    second = null;
    }
    
    public Tuple(F first, S second) {
        this.first = first;
        this.second = second;
    }
    
    @Override
    public boolean equals(Object e) {
        if (e instanceof Tuple) {
            Tuple<?, ?> t = (Tuple<?, ?>)(e);
            return t.first.equals(first) && t.second.equals(second);
        }
        else
            return false;
    }
    
    @Override
    public int hashCode() {
        return 31 * first.hashCode() + second.hashCode();
    }
}
