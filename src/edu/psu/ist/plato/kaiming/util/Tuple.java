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
}
