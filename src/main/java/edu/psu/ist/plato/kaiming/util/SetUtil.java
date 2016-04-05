package edu.psu.ist.plato.kaiming.util;

import java.util.HashSet;
import java.util.Set;

public class SetUtil {
    
    static public <T> Set<T> union(Set<T> u, Set<T> v) {
        Set<T> r = new HashSet<T>(u);
        r.addAll(v);
        return r;
    }
    
    static public <T> Set<T> difference(Set<T> u, Set<T> v) {
        Set<T> r = new HashSet<T>(u);
        r.removeAll(v);
        return r;
    }
    
    static public <T> Set<T> intersection(Set<T> u, Set<T> v) {
        Set<T> r = new HashSet<T>(u);
        r.retainAll(v);
        return r;
    }
}
