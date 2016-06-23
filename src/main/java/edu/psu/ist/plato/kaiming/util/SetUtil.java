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
    
    static public <T> Set<T> add(Set<T> u, T a) {
        Set<T> r = new HashSet<T>(u);
        r.add(a);
        return r;
    }
    
    static public <T> Set<T> remove(Set<T> u, T b) {
        Set<T> r = new HashSet<T>(u);
        r.remove(b);
        return r;
    }
}
