package edu.psu.ist.plato.kaiming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import edu.psu.ist.plato.kaiming.util.SetUtil;
import edu.psu.ist.plato.kaiming.util.Tuple;

public class Loop<T extends Entry> {
    private BasicBlock<T> mHeader;
    private Set<BasicBlock<T>> mBody;
    
    private Loop(BasicBlock<T> header, Set<BasicBlock<T>> loop) {
        mHeader = header;
        mBody = loop;
    }
    
    public static <T extends Entry> List<Loop<T>> detectLoops(CFG<T> cfg) {
        List<Loop<T>> ret = new ArrayList<>(); 
        
        Map<BasicBlock<T>, Set<BasicBlock<T>>> dominators = new HashMap<>();
        Set<BasicBlock<T>> tmp = new HashSet<>(), all;
        cfg.iterator().forEachRemaining(x -> tmp.add(x));
        all = Collections.unmodifiableSet(tmp);
        all.forEach(x -> dominators.put(x, all));
        List<Set<BasicBlock<T>>> singletons = all.stream().
                map(x -> Collections.unmodifiableSet(new HashSet<>(Arrays.asList(x)))).
                collect(Collectors.toList());
        boolean dirty = true;
        while (dirty) {
            dirty = false;
            for (Set<BasicBlock<T>> singleton : singletons) {
                BasicBlock<T> bb = singleton.iterator().next();
                Set<BasicBlock<T>> n =
                        bb.predecessors().stream().map(x -> dominators.get(x)).
                        reduce(SetUtil::intersection).orElse(singleton);
                
                if (!n.contains(bb))
                    n = SetUtil.add(n, bb);
                
                if (!n.equals(dominators.get(bb))) {
                    dirty = true;
                    dominators.put(bb, new HashSet<>(n));
                }
            }
        }
        List<Tuple<BasicBlock<T>, BasicBlock<T>>> backEdges = new ArrayList<>();
        for (Map.Entry<BasicBlock<T>, Set<BasicBlock<T>>> 
            entry : dominators.entrySet()) {
            BasicBlock<T> key = entry.getKey();
            Set<BasicBlock<T>> value = entry.getValue();
            backEdges.addAll(
                    SetUtil.intersection(new HashSet<>(key.successors()), value).
                    stream().map(x -> new Tuple<>(key, x)).collect(Collectors.toList()));
        }
        for (Tuple<BasicBlock<T>, BasicBlock<T>> t : backEdges) {
            ret.add(findLoopNodes(t, 
                    all.stream().filter(x -> dominators.get(x).contains(t.second)).
                    collect(Collectors.toSet())));
        }
        return ret;
    }
    
    private static <T extends Entry> Loop<T> findLoopNodes(
            Tuple<BasicBlock<T>, BasicBlock<T>> backEdge,
            Set<BasicBlock<T>> candidates) {
        Set<BasicBlock<T>> ret = new HashSet<>();
        ret.add(backEdge.first);
        ret.add(backEdge.second);
        for (BasicBlock<T> b : candidates) {
            if (ret.contains(b))
                continue;
            reachable(b, backEdge.first, candidates, ret, new HashSet<>());
        }
        return new Loop<T>(backEdge.second, ret);
    }
    
    private static <T extends Entry> boolean reachable(BasicBlock<T> start,
            BasicBlock<T> end, Set<BasicBlock<T>> candidates,
            Set<BasicBlock<T>> ret, Set<BasicBlock<T>> visited) {
        boolean canReach;
        if (start.equals(end)) {
            canReach = true;
        } else {
            canReach = false;
            visited.add(start);
            for (BasicBlock<T> s : start.successors()) {
                if (!candidates.contains(s))
                    continue;
                if (visited.contains(s)) {
                    canReach = canReach || ret.contains(s);
                    continue;
                }
                if (reachable(s, end, candidates, ret, visited)) {
                    canReach = true;
                }
            }
        }
        if (canReach)
            ret.add(start);
        return canReach;
    }
    
    public BasicBlock<T> header() {
        return mHeader;
    }
    
    public Set<BasicBlock<T>> loop() {
        return mBody;
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("(");
        b.append(mHeader.label());
        b.append(" : [ ");
        mBody.forEach(x -> { b.append(x.label()); b.append(" "); });
        b.append("])");
        return b.toString();
    }
}
