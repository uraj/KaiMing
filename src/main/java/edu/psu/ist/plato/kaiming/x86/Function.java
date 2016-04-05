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

public class Function extends Procedure<Instruction> {

    private Label mLabel;
    private CFG<Instruction> mCFG;
    private int mSubLabelCount;
    private boolean mHasIndirectJump;
    private static final String sSubLabelSuffix = "_sub";
    
    public Function(Label label, List<Instruction> insts) {
        mLabel = label;
        mCFG = buildCFG(insts);
        mSubLabelCount = 0;
    }

    public Label getLabel() {
        return mLabel;
    }
    
    @Override
    public CFG<Instruction> getCFG() {
        return mCFG;
    }
    
    public void setEntries(List<Instruction> entries) {
        mCFG = buildCFG(entries);
    }

    public List<Instruction> getInstructions() {
        return (List<Instruction>) getEntries();
    }

    public static BasicBlock<Instruction>
    searchContainingBlock(Collection<BasicBlock<Instruction>> bbs,
            long addr) {
        for (BasicBlock<Instruction> bb : bbs) {
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
    private CFG<Instruction> buildCFG(List<Instruction> entries) {
        Instruction[] inst = entries.toArray(new Instruction[0]);
        Arrays.sort(inst, Instruction.comparator);
        if (inst.length == 0) {
            return createCFGObject(new ArrayList<BasicBlock<Instruction>>(), null);
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
            BasicBlock<Instruction> init =
                    new BasicBlock<Instruction>(this, Arrays.asList(inst), mLabel);
            List<BasicBlock<Instruction>> bbs = new LinkedList<BasicBlock<Instruction>>();
            bbs.add(init);
            return createCFGObject(bbs, init);
        }

        Integer[] pa = pivots.toArray(new Integer[0]);
        ArrayList<BasicBlock<Instruction>> bbs = BasicBlock.split(this, entries, pa);
        bbs.get(0).setLable(mLabel);
        for (int i = 1; i < bbs.size(); ++i) {
            bbs.get(i).setLable(deriveSubLabel(bbs.get(i)));
        }
        
        mHasIndirectJump = false;
        for (int i = 0; i < bbs.size(); ++i) {
            Instruction in = bbs.get(i).getLastEntry();
            if (in.isJumpInst()) {
                JumpInst bin = (JumpInst)in;
                if (!bin.isIndirect() && bin.isTargetConcrete()) {
                    long targetAddr = bin.getTarget().getDisplacement();
                    BasicBlock<Instruction> targetBB = BasicBlock.searchContainingBlock(bbs, targetAddr);
                    if (targetBB == null)
                        continue;
                    Assert.test(targetBB.getFirstEntry().getIndex() == targetAddr, mLabel.getName());
                    bbs.get(i).addSuccessor(targetBB);
                    targetBB.addPredecessor(bbs.get(i));
                    if (!bin.isCondJumpInst()) {
                        continue;
                    }
                } else {
                    mHasIndirectJump = true;
                }
            }
            if (!in.isReturnInst() && i + 1 < bbs.size()) {
                bbs.get(i).addSuccessor(bbs.get(i + 1));
                bbs.get(i + 1).addPredecessor(bbs.get(i));
            }
        }
        
        List<BasicBlock<Instruction>> cfg = new ArrayList<BasicBlock<Instruction>>();
        if (!mHasIndirectJump) {
            cfg.add(bbs.get(0));
            for (int i = 1; i < bbs.size(); ++i) {
                if (bbs.get(i).hasPredecessor())
                    cfg.add(bbs.get(i));
            }
        } else {
            for (int i = 0; i < bbs.size(); ++i) {
                cfg.add(bbs.get(i));
            }
        }

        return createCFGObject(cfg, bbs.get(0));
    }

    @Override
    public String getName() {
        return mLabel.getName();
    }

    public Label deriveSubLabel(BasicBlock<Instruction> bb) {
        Assert.test(mLabel != null);
        Assert.test(mLabel.getName() != null);
        String name = mLabel.getName() + sSubLabelSuffix
                + String.valueOf(mSubLabelCount++);
        Label ret = new Label(name, 0);
        bb.getFirstEntry().fillLabelInformation(ret);
        return ret;
    }
}
