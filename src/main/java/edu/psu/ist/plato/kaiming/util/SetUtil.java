package edu.psu.ist.plato.kaiming.util;

import java.util.HashSet;
import java.util.Set;

public class SetUtil {
    static public <T> Set<T> union(Set<T> u, Set<T> v) {
        Set<T> r = new HashSet<T>();
        r.addAll(u);
        r.addAll(v);
        return r;
    }
}
