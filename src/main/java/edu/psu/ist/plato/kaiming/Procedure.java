package edu.psu.ist.plato.kaiming;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.psu.ist.plato.kaiming.util.Assert;

public abstract class Procedure<T extends Entry> {
    
    /**
     * A procedure is required to have a CFG, even if it is only a trivial one.
     */
    public abstract CFG<T> cfg();
    
    protected Label mLabel;
    
    public Label label() {
    	return mLabel;
    };
    
    protected boolean mHasIndirectJump;
    
    public List<T> entries() {
        return cfg().entries();
    }

    protected CFG<T> createCFGObject(Collection<BasicBlock<T>> bbs,
            BasicBlock<T> entry) {
        return new CFG<T>(bbs, entry);
    }
    
    public abstract String name();
    
    protected abstract Label deriveSubLabel(BasicBlock<T> bb);
    
    // FIXME: Take conditional instructions into consideration
    protected CFG<T> buildCFG(List<T> entries) {
        Entry[] inst = entries.toArray(new Entry[0]);
        Arrays.sort(inst, T.comparator);
        if (inst.length == 0) {
            return createCFGObject(new ArrayList<BasicBlock<T>>(), null);
        }

        Set<Integer> pivots = new TreeSet<Integer>();
        for (int i = 0; i < inst.length; ++i) {
            if (inst[i].isTerminator()) {
                Entry.Terminator<?> term = (Entry.Terminator<?>)inst[i];
                if (!term.isCall())
                    pivots.add(i + 1);
                if (term.isIntraprocedural()) {
                    if (!term.isIndirect() && term.isTargetConcrete()) {
                        long target = term.targetIndex();
                        int idx = Entry.searchIndex(inst, target);
                        if (idx != -1) {
                            pivots.add(idx);
                        }
                    }
                }
            }
        }

        if (pivots.isEmpty()) {
            BasicBlock<T> init =
                    new BasicBlock<T>(this, entries, mLabel);
            List<BasicBlock<T>> bbs = new LinkedList<BasicBlock<T>>();
            bbs.add(init);
            return createCFGObject(bbs, init);
        }

        Integer[] pa = pivots.toArray(new Integer[0]);
        ArrayList<BasicBlock<T>> bbs = BasicBlock.split(this, entries, pa);
        bbs.get(0).setLable(mLabel);
        for (int i = 1; i < bbs.size(); ++i) {
            bbs.get(i).setLable(deriveSubLabel(bbs.get(i)));
        }
        
        mHasIndirectJump = false;
        for (int i = 0; i < bbs.size(); ++i) {
            T in = bbs.get(i).lastEntry();
            if (in.isTerminator()) {
            	@SuppressWarnings("unchecked")
				Entry.Terminator<T> term = (Entry.Terminator<T>)in;
            	if (term.isIntraprocedural()) {
            	    if (!term.isIndirect() && term.isTargetConcrete()) {
            	        long targetAddr = term.targetIndex();
            	        BasicBlock<T> targetBB = BasicBlock.searchContainingBlock(bbs, targetAddr);
            	        if (targetBB != null) { // target may be outside of the function. Strange, but it happens
            	            Assert.verify(targetBB.firstEntry().index() == targetAddr);
            	            term.relocateTarget(targetBB);
            	            bbs.get(i).addSuccessor(targetBB);
            	            targetBB.addPredecessor(bbs.get(i));
            	        }
            	        if (!term.isConditional()) {
            	            continue;
            	        }
            	    } else {
            	        mHasIndirectJump = true;
            	    }
            	}
            }
            if (!(in.isTerminator() && ((Entry.Terminator<?>)in).isReturn()) // not a return 
            		&& i + 1 < bbs.size()) {
                bbs.get(i).addSuccessor(bbs.get(i + 1));
                bbs.get(i + 1).addPredecessor(bbs.get(i));
            }
        }
        
        List<BasicBlock<T>> cfg = new ArrayList<BasicBlock<T>>();
        if (!mHasIndirectJump) {
            cfg.add(bbs.get(0));
            for (int i = 1; i < bbs.size(); ++i) {
                BasicBlock<T> inspect = bbs.get(i);
                if (inspect.hasPredecessor())
                    cfg.add(inspect);
                else {
                    inspect.allSuccessor().forEach(
                            succ -> succ.removePredecessor(inspect));
                }
            }
        } else {
            cfg.addAll(bbs);
        }
        checkCFGSanity(bbs);
        return createCFGObject(cfg, bbs.get(0));
    }

    private void checkCFGSanity(List<BasicBlock<T>> bbs) {
        Set<BasicBlock<T>> all = new HashSet<BasicBlock<T>>(bbs);
        for (BasicBlock<T> bb : bbs) {
            bb.allPredecessor().forEach(pred -> Assert.test(all.contains(pred)));
            bb.allSuccessor().forEach(succ -> Assert.test(all.contains(succ)));
        }
    }
}
