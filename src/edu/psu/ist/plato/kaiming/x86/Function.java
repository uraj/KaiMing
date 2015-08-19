package edu.psu.ist.plato.kaiming.x86;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.psu.ist.plato.kaiming.BasicBlock;
import edu.psu.ist.plato.kaiming.CFG;
import edu.psu.ist.plato.kaiming.Entry;
import edu.psu.ist.plato.kaiming.Label;
import edu.psu.ist.plato.kaiming.Procedure;
import edu.psu.ist.plato.kaiming.util.Assert;

public class Function extends Procedure {

    private int mSubLabelCount;
    private static final String sSubLabelSuffix = "_sub";

    public Function(AsmLabel label, List<Instruction> insts) {
        super(label, insts);
        mSubLabelCount = 0;
    }

    public AsmLabel getLabel() {
        return (AsmLabel)mLabel;
    }

    @SuppressWarnings("unchecked")
    public List<Instruction> getInstructions() {
        return (List<Instruction>) getEntries();
    }

    public static BasicBlock searchContainingBlock(Collection<BasicBlock> bbs,
            long addr) {
        for (BasicBlock bb : bbs) {
            if (bb.getLastEntry().compareTo(addr) >= 0
                    && bb.getFirstEntry().compareTo(addr) <= 0) {
                return bb;
            }
        }
        return null;
    }
    
    public static int searchContainingBlock(BasicBlock[] bbs,
            long addr) {
        for (int i = 0; i < bbs.length; ++i) {
            if (bbs[i].getLastEntry().compareTo(addr) >= 0
                    && bbs[i].getFirstEntry().compareTo(addr) <= 0) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public CFG buildCFGInternal(List<? extends Entry> entries) {
        Instruction[] inst = entries.toArray(new Instruction[0]);
        Arrays.sort(inst, Instruction.comparator);
        if (inst.length == 0) {
            return createCFGObject(new ArrayList<BasicBlock>(), null);
        }

        Set<Integer> pivots = new TreeSet<Integer>();
        for (int i = 0; i < inst.length; ++i) {
            if (inst[i].isTerminator()) {
                pivots.add(i + 1);
            }
            if (inst[i].isBranchInst() && !inst[i].isCallInst()) {
                BranchInst bin = (BranchInst)inst[i];
                if (!bin.isIndirect() && bin.isTargetConcrete()) {
                    long target = bin.getTarget().getDisplacement();
                    int idx = Entry.binSearch(inst, target);
                    if (idx != -1)
                        pivots.add(idx);
                }
            }
        }

        if (pivots.isEmpty()) {
            BasicBlock init = new BasicBlock(this, Arrays.asList(inst), mLabel);
            List<BasicBlock> bbs = new LinkedList<BasicBlock>();
            bbs.add(init);
            return createCFGObject(bbs, init);
        }

        Integer[] pa = pivots.toArray(new Integer[0]);
        BasicBlock[] bbs = BasicBlock.split(this, entries, pa);
        bbs[0].setLable(mLabel);
        for (int i = 1; i < bbs.length; ++i) {
            bbs[i].setLable(deriveSubLabel(bbs[i]));
        }
        
        boolean hasIndirectJump = false;
        for (int i = 0; i < bbs.length; ++i) {
            Instruction in = (Instruction)bbs[i].getLastEntry();
            if (in.isBranchInst() && !in.isCallInst()) {
                BranchInst bin = (BranchInst)in;
                if (!bin.isIndirect() && bin.isTargetConcrete()) {
                    long targetAddr = bin.getTarget().getDisplacement();
                    int idx = Function.searchContainingBlock(bbs, targetAddr);
                    if (idx == -1)
                        continue;
                    BasicBlock targetBb = bbs[idx];
                    Assert.test(targetBb.getFirstEntry().getIndex() == targetAddr, mLabel.getName());
                    bbs[i].addSuccessor(targetBb);
                    targetBb.addPredecessor(bbs[i]);
                    if (!bin.isCondJumpInst()) {
                        continue;
                    }
                } else {
                    hasIndirectJump = true;
                }
            }
            if (!in.isReturnInst() && i + 1 < bbs.length) {
                bbs[i].addSuccessor(bbs[i + 1]);
                bbs[i + 1].addPredecessor(bbs[i]);
            }
        }
        
        List<BasicBlock> cfg = new ArrayList<BasicBlock>();
        if (!hasIndirectJump) {
            cfg.add(bbs[0]);
            for (int i = 1; i < bbs.length; ++i) {
                if (bbs[i].hasPredecessor())
                    cfg.add(bbs[i]);
            }
        } else {
            for (int i = 0; i < bbs.length; ++i) {
                cfg.add(bbs[i]);
            }
        }

        return createCFGObject(cfg, bbs[0]);
    }

    @Override
    public String getName() {
        return mLabel.getName();
    }

    @Override
    public Label deriveSubLabel(BasicBlock bb) {
        Assert.test(mLabel != null);
        Assert.test(mLabel.getName() != null);
        String name = mLabel.getName() + sSubLabelSuffix
                + String.valueOf(mSubLabelCount++);
        AsmLabel ret = new AsmLabel(name, 0);
        bb.getFirstEntry().fillLabelInformation(ret);
        return ret;
    }
}
