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

    private AsmLabel mLabel;
    private CFG mCFG;
    private int mSubLabelCount;
    private boolean mHasIndirectJump;
    private static final String sSubLabelSuffix = "_sub";
    
    public Function(AsmLabel label, List<Instruction> insts) {
        mLabel = label;
        mCFG = buildCFG(insts);
        mSubLabelCount = 0;
    }

    public AsmLabel getLabel() {
        return (AsmLabel)mLabel;
    }
    
    @Override
    public CFG getCFG() {
        return mCFG;
    }
    
    public void setEntries(List<? extends Entry> entries) {
        mCFG = buildCFG(entries);
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
    
    public boolean hasIndirectJump() {
        return mHasIndirectJump;
    }

    // TODO: Take conditional move into consideration
    private CFG buildCFG(List<? extends Entry> entries) {
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
                    int idx = Entry.searchIndex(inst, target);
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
        
        mHasIndirectJump = false;
        for (int i = 0; i < bbs.length; ++i) {
            Instruction in = (Instruction)bbs[i].getLastEntry();
            if (in.isJumpInst()) {
                JumpInst bin = (JumpInst)in;
                if (!bin.isIndirect() && bin.isTargetConcrete()) {
                    long targetAddr = bin.getTarget().getDisplacement();
                    BasicBlock targetBB = BasicBlock.searchContainingBlock(bbs, targetAddr);
                    if (targetBB == null)
                        continue;
                    Assert.test(targetBB.getFirstEntry().getIndex() == targetAddr, mLabel.getName());
                    bbs[i].addSuccessor(targetBB);
                    targetBB.addPredecessor(bbs[i]);
                    if (!bin.isCondJumpInst()) {
                        continue;
                    }
                } else {
                    mHasIndirectJump = true;
                }
            }
            if (!in.isReturnInst() && i + 1 < bbs.length) {
                bbs[i].addSuccessor(bbs[i + 1]);
                bbs[i + 1].addPredecessor(bbs[i]);
            }
        }
        
        List<BasicBlock> cfg = new ArrayList<BasicBlock>();
        if (!mHasIndirectJump) {
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

    public Label deriveSubLabel(BasicBlock bb) {
        Assert.test(mLabel != null);
        Assert.test(mLabel.getName() != null);
        String name = mLabel.getName() + sSubLabelSuffix
                + String.valueOf(mSubLabelCount++);
        AsmLabel ret = new AsmLabel(name, 0);
        ((Instruction)bb.getFirstEntry()).fillLabelInformation(ret);
        return ret;
    }
}
